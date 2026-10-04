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
    private final Label resultLabel = new Label("Result will appear here.");
    private final ListView<String> recordsList = new ListView<>();

    @Override
    public void start(Stage stage) {
        createDatabase();

        fromUnitBox.setValue("Celsius");
        toUnitBox.setValue("Fahrenheit");
        temperatureField.setPromptText("Enter temperature");

        Button convertButton = new Button("Convert");
        convertButton.setOnAction(event -> convertTemperature());

        Button saveButton = new Button("Save to database");
        saveButton.setOnAction(event -> saveRecord());

        Button refreshButton = new Button("Refresh saved records");
        refreshButton.setOnAction(event -> loadRecords());

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);

        form.add(new Label("Temperature:"), 0, 0);
        form.add(temperatureField, 1, 0);
        form.add(new Label("From:"), 0, 1);
        form.add(fromUnitBox, 1, 1);
        form.add(new Label("To:"), 0, 2);
        form.add(toUnitBox, 1, 2);

        HBox buttons = new HBox(10, convertButton, saveButton, refreshButton);

        VBox root = new VBox(
                15,
                new Label("Temperature Converter"),
                form,
                buttons,
                resultLabel,
                new Label("Saved temperature records:"),
                recordsList
        );
        root.setPadding(new Insets(20));

        loadRecords();

        Scene scene = new Scene(root, 520, 500);
        stage.setTitle("Temperature Converter");
        stage.setScene(scene);
        stage.show();
    }

    private void convertTemperature() {
        try {
            double input = Double.parseDouble(temperatureField.getText());
            double result = convert(input, fromUnitBox.getValue(), toUnitBox.getValue());

            resultLabel.setText(String.format(
                    "%.2f %s = %.2f %s",
                    input,
                    fromUnitBox.getValue(),
                    result,
                    toUnitBox.getValue()
            ));
        } catch (NumberFormatException exception) {
            showError("Please enter a valid number.");
        }
    }

    private double convert(double value, String from, String to) {
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
        String unitTable = """
                CREATE TABLE IF NOT EXISTS temperature_units (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE,
                    symbol TEXT NOT NULL
                )
                """;

        String recordTable = """
                CREATE TABLE IF NOT EXISTS temperature_records (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    input_value REAL NOT NULL,
                    from_unit_id INTEGER NOT NULL,
                    to_unit_id INTEGER NOT NULL,
                    result_value REAL NOT NULL,
                    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (from_unit_id) REFERENCES temperature_units(id),
                    FOREIGN KEY (to_unit_id) REFERENCES temperature_units(id)
                )
                """;

        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             Statement statement = connection.createStatement()) {

            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute(unitTable);
            statement.execute(recordTable);

            statement.executeUpdate(
                    "INSERT OR IGNORE INTO temperature_units(name, symbol) VALUES " +
                            "('Celsius', '°C'), " +
                            "('Fahrenheit', '°F'), " +
                            "('Kelvin', 'K')"
            );
        } catch (Exception exception) {
            showError("Database could not be created: " + exception.getMessage());
        }
    }

    private void saveRecord() {
        try {
            double input = Double.parseDouble(temperatureField.getText());
            double result = convert(input, fromUnitBox.getValue(), toUnitBox.getValue());

            String sql = """
                    INSERT INTO temperature_records
                    (input_value, from_unit_id, to_unit_id, result_value)
                    VALUES (?, ?, ?, ?)
                    """;

            try (Connection connection = DriverManager.getConnection(DATABASE_URL);
                 PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setDouble(1, input);
                statement.setInt(2, getUnitId(connection, fromUnitBox.getValue()));
                statement.setInt(3, getUnitId(connection, toUnitBox.getValue()));
                statement.setDouble(4, result);
                statement.executeUpdate();
            }

            loadRecords();
            resultLabel.setText("Record saved successfully.");
        } catch (NumberFormatException exception) {
            showError("Please enter a valid number before saving.");
        } catch (Exception exception) {
            showError("Record could not be saved: " + exception.getMessage());
        }
    }

    private int getUnitId(Connection connection, String unitName) throws Exception {
        String sql = "SELECT id FROM temperature_units WHERE name = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, unitName);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("id");
                }
            }
        }

        throw new Exception("Temperature unit not found.");
    }

    private void loadRecords() {
        recordsList.getItems().clear();

        String sql = """
                SELECT r.input_value, r.result_value, r.created_at,
                       f.symbol AS from_symbol, t.symbol AS to_symbol
                FROM temperature_records r
                JOIN temperature_units f ON r.from_unit_id = f.id
                JOIN temperature_units t ON r.to_unit_id = t.id
                ORDER BY r.id DESC
                """;

        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                recordsList.getItems().add(String.format(
                        "%.2f %s = %.2f %s  |  %s",
                        resultSet.getDouble("input_value"),
                        resultSet.getString("from_symbol"),
                        resultSet.getDouble("result_value"),
                        resultSet.getString("to_symbol"),
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