#include <iostream>
#include <vector>
#include <algorithm>

using namespace std;

// Observer Interface
class Observer {
public:
    virtual void update(float temperature, float humidity, float pressure) = 0;
    virtual ~Observer() {}
};

// Subject Interface
class Subject {
public:
    virtual void attach(Observer* observer) = 0;
    virtual void detach(Observer* observer) = 0;
    virtual void notify() = 0;
    virtual ~Subject() {}
};

// Concrete Subject
class WeatherStation : public Subject {
private:
    vector<Observer*> observers;

    float temperature;
    float humidity;
    float pressure;

public:
    void attach(Observer* observer) override {
        observers.push_back(observer);
    }

    void detach(Observer* observer) override {
        observers.erase(
            remove(observers.begin(), observers.end(), observer),
            observers.end()
        );
    }

    void notify() override {
        for (Observer* observer : observers) {
            observer->update(temperature, humidity, pressure);
        }
    }

    void setMeasurements(float t, float h, float p) {
        temperature = t;
        humidity = h;
        pressure = p;

        notify();
    }
};

// Concrete Observer 1
class CurrentWeatherDisplay : public Observer {
public:
    void update(float temperature, float humidity, float pressure) override {
        cout << "\n[Current Weather Display]" << endl;
        cout << "Temperature: " << temperature << " C" << endl;
        cout << "Humidity: " << humidity << "%" << endl;
        cout << "Pressure: " << pressure << " hPa" << endl;
    }
};

// Concrete Observer 2
class StatisticsDisplay : public Observer {
private:
    float temperatureSum = 0;
    int readings = 0;

public:
    void update(float temperature, float humidity, float pressure) override {
        temperatureSum += temperature;
        readings++;

        cout << "\n[Statistics Display]" << endl;
        cout << "Average Temperature: "
             << temperatureSum / readings << " C" << endl;
    }
};

// Concrete Observer 3
class MobileWeatherDisplay : public Observer {
public:
    void update(float temperature, float humidity, float pressure) override {
        cout << "\n[Mobile Weather Display]" << endl;
        cout << "Temperature: " << temperature << " C" << endl;
        cout << "Humidity: " << humidity << "%" << endl;
    }
};

// Client
int main() {

    WeatherStation weatherStation;

    CurrentWeatherDisplay currentDisplay;
    StatisticsDisplay statisticsDisplay;
    MobileWeatherDisplay mobileDisplay;

    weatherStation.attach(&currentDisplay);
    weatherStation.attach(&statisticsDisplay);
    weatherStation.attach(&mobileDisplay);

    cout << "=== First Weather Update ===" << endl;
    weatherStation.setMeasurements(30.0, 70.0, 1012.0);

    cout << "\n=== Second Weather Update ===" << endl;
    weatherStation.setMeasurements(32.0, 65.0, 1010.0);

    cout << "\n=== Removing Mobile Display ===" << endl;
    weatherStation.detach(&mobileDisplay);

    cout << "\n=== Third Weather Update ===" << endl;
    weatherStation.setMeasurements(28.0, 75.0, 1015.0);

    return 0;
}