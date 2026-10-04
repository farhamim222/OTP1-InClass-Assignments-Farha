import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class Main extends Application {

    private static final String DATABASE_URL = "jdbc:sqlite:temperature.db";

    private final TextField temperatureField = new TextField();
    private final ComboBox<String> fromUnitBox =
            new ComboBox<>(FXCollections.observableArrayList("Celsius", "Fahrenheit", "Kelvin"));
    private final ComboBox<String> toUnitBox =
            new ComboBox<>(FXCollections.observableArrayList("Celsius", "Fahrenheit", "Kelvin"));
    private final Label conversionResultLabel = new Label("Result will appear here.");

    private final TextField speedField = new TextField();
    private final TextField distanceField = new TextField();
    private final TextField timeField = new TextField();
    private final Label databaseResultLabel = new Label("No record saved yet.");
    private final ListView<String> recordsList = new ListView<>();

    @Override
    public void start(Stage stage) {
        createDatabase();

        fromUnitBox.setValue("Celsius");
        toUnitBox.setValue("Fahrenheit");

        temperatureField.setPromptText("Enter temperature");
        speedField.setPromptText("e.g. 60");
        distanceField.setPromptText("e.g. 120");
        timeField.setPromptText("e.g. 2");

        Button convertButton = new Button("Convert temperature");
        convertButton.setOnAction(event -> convertTemperature());

        Button saveButton = new Button("Save speed/distance/time");
        saveButton.setOnAction(event -> saveSpeedDistanceTimeRecord());

        Button refreshButton = new Button("Refresh records");
        refreshButton.setOnAction(event -> loadRecords());

        GridPane temperatureForm = new GridPane();
        temperatureForm.setHgap(10);
        temperatureForm.setVgap(10);
        temperatureForm.add(new Label("Temperature:"), 0, 0);
        temperatureForm.add(temperatureField, 1, 0);
        temperatureForm.add(new Label("From:"), 0, 1);
        temperatureForm.add(fromUnitBox, 1, 1);
        temperatureForm.add(new Label("To:"), 0, 2);
        temperatureForm.add(toUnitBox, 1, 2);

        GridPane databaseForm = new GridPane();
        databaseForm.setHgap(10);
        databaseForm.setVgap(10);
        databaseForm.add(new Label("Speed (km/h):"), 0, 0);
        databaseForm.add(speedField, 1, 0);
        databaseForm.add(new Label("Distance (km):"), 0, 1);
        databaseForm.add(distanceField, 1, 1);
        databaseForm.add(new Label("Time (hours):"), 0, 2);
        databaseForm.add(timeField, 1, 2);

        HBox databaseButtons = new HBox(10, saveButton, refreshButton);

        VBox root = new VBox(
                15,
                new Label("Temperature Converter"),
                temperatureForm,
                convertButton,
                conversionResultLabel,
                new Label("Speed, Distance and Time Database"),
                databaseForm,
                databaseButtons,
                databaseResultLabel,
                new Label("Saved records:"),
                recordsList
        );

        root.setPadding(new Insets(20));
        loadRecords();

        Scene scene = new Scene(root, 650, 700);
        stage.setTitle("Temperature Converter");
        stage.setScene(scene);
        stage.show();
    }

    private void convertTemperature() {
        try {
            double input = Double.parseDouble(temperatureField.getText());
            double result = convertTemperatureValue(input, fromUnitBox.getValue(), toUnitBox.getValue());

            conversionResultLabel.setText(String.format(
                    "%.2f %s = %.2f %s",
                    input,
                    fromUnitBox.getValue(),
                    result,
                    toUnitBox.getValue()
            ));
        } catch (NumberFormatException exception) {
            showError("Please enter a valid temperature number.");
        }
    }

    public static double convertTemperatureValue(double value, String from, String to) {
        double celsius;

        switch (from) {
            case "Celsius" -> celsius = value;
            case "Fahrenheit" -> celsius = (value - 32) * 5 / 9;
            case "Kelvin" -> celsius = value - 273.15;
            default -> throw new IllegalArgumentException("Unknown unit");
        }

        return switch (to) {
            case "Celsius" -> celsius;
            case "Fahrenheit" -> celsius * 9 / 5 + 32;
            case "Kelvin" -> celsius + 273.15;
            default -> throw new IllegalArgumentException("Unknown unit");
        };
    }

    private void createDatabase() {
        String sessionsTable = """
                CREATE TABLE IF NOT EXISTS activity_sessions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    created_at TEXT DEFAULT CURRENT_TIMESTAMP
                )
                """;

        String recordsTable = """
                CREATE TABLE IF NOT EXISTS speed_distance_time_records (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    session_id INTEGER NOT NULL,
                    speed_kmh REAL NOT NULL,
                    distance_km REAL NOT NULL,
                    time_hours REAL NOT NULL,
                    FOREIGN KEY (session_id) REFERENCES activity_sessions(id)
                )
                """;

        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             Statement statement = connection.createStatement()) {

            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute(sessionsTable);
            statement.execute(recordsTable);
        } catch (Exception exception) {
            showError("Database could not be created: " + exception.getMessage());
        }
    }

    private void saveSpeedDistanceTimeRecord() {
        try {
            double speed = Double.parseDouble(speedField.getText());
            double distance = Double.parseDouble(distanceField.getText());
            double time = Double.parseDouble(timeField.getText());

            if (speed < 0 || distance < 0 || time <= 0) {
                showError("Speed and distance must be 0 or more. Time must be greater than 0.");
                return;
            }

            try (Connection connection = DriverManager.getConnection(DATABASE_URL)) {
                connection.setAutoCommit(false);

                int sessionId;
                try (PreparedStatement sessionStatement = connection.prepareStatement(
                        "INSERT INTO activity_sessions DEFAULT VALUES",
                        Statement.RETURN_GENERATED_KEYS)) {

                    sessionStatement.executeUpdate();

                    try (ResultSet generatedKeys = sessionStatement.getGeneratedKeys()) {
                        generatedKeys.next();
                        sessionId = generatedKeys.getInt(1);
                    }
                }

                String sql = """
                        INSERT INTO speed_distance_time_records
                        (session_id, speed_kmh, distance_km, time_hours)
                        VALUES (?, ?, ?, ?)
                        """;

                try (PreparedStatement recordStatement = connection.prepareStatement(sql)) {
                    recordStatement.setInt(1, sessionId);
                    recordStatement.setDouble(2, speed);
                    recordStatement.setDouble(3, distance);
                    recordStatement.setDouble(4, time);
                    recordStatement.executeUpdate();
                }

                connection.commit();
            }

            databaseResultLabel.setText("Speed, distance and time record saved successfully.");
            loadRecords();
        } catch (NumberFormatException exception) {
            showError("Please enter valid numbers for speed, distance and time.");
        } catch (Exception exception) {
            showError("Record could not be saved: " + exception.getMessage());
        }
    }

    private void loadRecords() {
        recordsList.getItems().clear();

        String sql = """
                SELECT r.speed_kmh, r.distance_km, r.time_hours, s.created_at
                FROM speed_distance_time_records r
                JOIN activity_sessions s ON r.session_id = s.id
                ORDER BY r.id DESC
                """;

        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                recordsList.getItems().add(String.format(
                        "Speed: %.2f km/h | Distance: %.2f km | Time: %.2f h | %s",
                        resultSet.getDouble("speed_kmh"),
                        resultSet.getDouble("distance_km"),
                        resultSet.getDouble("time_hours"),
                        resultSet.getString("created_at")
                ));
            }
        } catch (Exception exception) {
            showError("Records could not be loaded: " + exception.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Temperature Converter");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}