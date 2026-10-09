# Temperature Converter

## 1. Assignment Description

This project is a JavaFX Temperature Converter application. It converts values between Celsius, Fahrenheit, and Kelvin.

The application also stores speed, distance, and time records in an SQLite database. The database uses two related tables: `activity_sessions` and `speed_distance_time_records`.

## 2. Technologies and Tools Used

- Java 21
- JavaFX 21.0.4
- Maven
- SQLite
- SQLite JDBC driver
- JUnit 5
- JaCoCo
- Jenkins
- Docker
- IntelliJ IDEA
- Git and GitHub

## 3. Design Approach and Implementation

The application uses a JavaFX graphical user interface.

The user can enter a temperature value, select the input unit and output unit, and convert the value.

The application also allows the user to enter speed, distance, and time values and save them to the SQLite database.

The database contains these related tables:

- `activity_sessions`: stores a session ID and creation time.
- `speed_distance_time_records`: stores speed, distance, and time values. Each record is connected to an activity session using a foreign key.

## 4. Testing and Quality Assurance

Unit tests were created using JUnit 5.

The tests verify:

- Celsius to Fahrenheit conversion
- Fahrenheit to Celsius conversion
- Kelvin to Celsius conversion
- Invalid temperature unit handling

JaCoCo is used to create a code coverage report.

The project was tested with:

```bash
mvn clean test