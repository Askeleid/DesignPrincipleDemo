import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

// Immutable record representing a database row
class AccountRecord {
    private final int id;
    private final String owner;
    private final double balance;

    public AccountRecord(int id, String owner, double balance) {
        this.id = id;
        this.owner = owner;
        this.balance = balance;
    }

    public int getId() { return id; }
    public String getOwner() { return owner; }
    public double getBalance() { return balance; }
}

// 1. Narrow Memento Interface (Visible to Caretaker)
interface IMemento {
    String getSavepointName();
    String getTimestamp();
}

// 2. Originator (In-Memory Database Engine)
class DatabaseEngine {
    private Map<Integer, AccountRecord> accountsTable = new LinkedHashMap<>();
    private int transactionSequence = 100;

    // 3. Concrete Memento: Private static inner class locks state to Originator
    private static class DatabaseMemento implements IMemento {
        private final Map<Integer, AccountRecord> tableSnapshot;
        private final int txSequenceSnapshot;
        private final String savepointName;
        private final String timestamp;

        private DatabaseMemento(Map<Integer, AccountRecord> table, int seq, String name, String time) {
            this.tableSnapshot = new LinkedHashMap<>(table);
            this.txSequenceSnapshot = seq;
            this.savepointName = name;
            this.timestamp = time;
        }

        private Map<Integer, AccountRecord> getTableSnapshot() {
            return new LinkedHashMap<>(tableSnapshot);
        }

        private int getTxSequenceSnapshot() {
            return txSequenceSnapshot;
        }

        @Override
        public String getSavepointName() {
            return savepointName;
        }

        @Override
        public String getTimestamp() {
            return timestamp;
        }
    }

    public void insertAccount(int id, String owner, double initialBalance) {
        if (accountsTable.containsKey(id)) {
            throw new IllegalArgumentException("Constraint Violation: Duplicate Account ID " + id);
        }
        if (initialBalance < 0.0) {
            throw new IllegalArgumentException("Constraint Violation: Initial balance cannot be negative.");
        }
        accountsTable.put(id, new AccountRecord(id, owner, initialBalance));
        transactionSequence++;
        System.out.printf("  [DB QUERY] Inserted Account #%d (%s) with $%.2f%n", id, owner, initialBalance);
    }

    public void updateBalance(int id, double newBalance) {
        AccountRecord existing = accountsTable.get(id);
        if (existing == null) {
            throw new IllegalStateException("Query Error: Account ID " + id + " does not exist.");
        }
        if (newBalance < 0.0) {
            throw new IllegalStateException(String.format(
                "Constraint Violation: Insufficient funds for Account #%d (Attempted balance: $%.2f)", id, newBalance
            ));
        }
        accountsTable.put(id, new AccountRecord(id, existing.getOwner(), newBalance));
        transactionSequence++;
        System.out.printf("  [DB QUERY] Updated Account #%d balance to $%.2f%n", id, newBalance);
    }

    public void deleteAccount(int id) {
        if (accountsTable.remove(id) == null) {
            throw new IllegalStateException("Query Error: Cannot delete non-existent Account ID " + id);
        }
        transactionSequence++;
        System.out.printf("  [DB QUERY] Deleted Account #%d%n", id);
    }

    // Creates an encapsulated Memento snapshot
    public IMemento createSavepoint(String savepointName, String timestamp) {
        return new DatabaseMemento(this.accountsTable, this.transactionSequence, savepointName, timestamp);
    }

    // Restores database state from a Memento snapshot
    public void restoreSavepoint(IMemento memento) {
        if (!(memento instanceof DatabaseMemento)) {
            throw new IllegalArgumentException("Invalid Memento snapshot provided for restoration.");
        }
        DatabaseMemento concreteMemento = (DatabaseMemento) memento;
        this.accountsTable = concreteMemento.getTableSnapshot();
        this.transactionSequence = concreteMemento.getTxSequenceSnapshot();
    }

    public void printTableState() {
        System.out.println("\n+----------------------------------------------------+");
        System.out.printf("| DATABASE STATE (Tx Sequence: %-4d)                 |%n", transactionSequence);
        System.out.println("+--------+----------------------+--------------------+");
        System.out.println("| ID     | Owner                | Balance ($)        |");
        System.out.println("+--------+----------------------+--------------------+");
        for (AccountRecord record : accountsTable.values()) {
            System.out.printf("| %-6d | %-20s | %-18.2f |%n",
                record.getId(), record.getOwner(), record.getBalance());
        }
        System.out.println("+--------+----------------------+--------------------+\n");
    }
}

// 4. Caretaker (Transaction Manager)
class TransactionManager {
    private final Deque<IMemento> savepointHistory = new ArrayDeque<>();
    private final DatabaseEngine dbEngine;

    public TransactionManager(DatabaseEngine dbEngine) {
        this.dbEngine = dbEngine;
    }

    public void createSavepoint(String name, String time) {
        IMemento snapshot = dbEngine.createSavepoint(name, time);
        savepointHistory.push(snapshot);
        System.out.printf("[TX MANAGER] Savepoint created: '%s' at %s%n", name, time);
    }

    public boolean rollbackToLastSavepoint() {
        if (savepointHistory.isEmpty()) {
            System.out.println("[TX MANAGER] No savepoints available for rollback!");
            return false;
        }

        IMemento lastSnapshot = savepointHistory.pop();
        System.out.printf("[TX MANAGER] Rolling back database to savepoint: '%s' (%s)...%n",
            lastSnapshot.getSavepointName(), lastSnapshot.getTimestamp());

        dbEngine.restoreSavepoint(lastSnapshot);
        return true;
    }

    public void commitAndClearHistory() {
        savepointHistory.clear();
        System.out.println("[TX MANAGER] Transaction committed. Savepoint history cleared.");
    }

    public void printAuditLog() {
        System.out.printf("[TX MANAGER] Active Savepoints in Stack (%d):%n", savepointHistory.size());
        int index = 1;
        for (IMemento memento : savepointHistory) {
            System.out.printf("  %d. %s [Timestamp: %s]%n",
                index++, memento.getSavepointName(), memento.getTimestamp());
        }
    }
}

// 5. Client / Driver Demonstration
public class MementoPatternDemo {
    public static void main(String[] args) {
        DatabaseEngine db = new DatabaseEngine();
        TransactionManager txManager = new TransactionManager(db);

        System.out.println("=== STEP 1: Initializing Database Accounts ===");
        db.insertAccount(101, "Mr. Rahman", 5000.00);
        db.insertAccount(102, "Mr. Sarker", 3000.00);
        db.printTableState();

        // Create savepoint before executing a multi-step batch transfer
        txManager.createSavepoint("SP_BEFORE_BATCH_TRANSFER", "10:15:00 AM");
        txManager.printAuditLog();

        System.out.println("\n=== STEP 2: Executing Atomic Batch Transfer (Fails Mid-Way) ===");
        try {
            // Operation 1: Deduct $2000 from Rahman (Succeeds)
            db.updateBalance(101, 3000.00);

            // Operation 2: Insert new merchant account (Succeeds)
            db.insertAccount(103, "TechStore Ltd", 2000.00);

            System.out.print("\n--- Dirty State Inside Uncommitted Transaction ---");
            db.printTableState();

            // Operation 3: Deduct $4500 from Mr. Sarker who only has $3000 (Throws Exception!)
            System.out.println("  [DB QUERY] Attempting to deduct $4500 from Account #102...");
            db.updateBalance(102, -1500.00);

            // Commit if all operations succeed
            txManager.commitAndClearHistory();
        } catch (RuntimeException e) {
            System.out.println("\n  [ERROR CAUGHT] " + e.getMessage());
            System.out.println("  [SYSTEM] Aborting transaction to preserve ACID Atomicity!");
            txManager.rollbackToLastSavepoint();
        }

        System.out.println("\n=== STEP 3: Database State After Automatic Rollback ===");
        db.printTableState();
    }
}