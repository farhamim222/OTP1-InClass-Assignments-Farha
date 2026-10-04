import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MainTest {

    @Test
    void convertsCelsiusToFahrenheit() {
        assertEquals(
                212.0,
                Main.convertTemperatureValue(100, "Celsius", "Fahrenheit"),
                0.001
        );
    }

    @Test
    void convertsKelvinToCelsius() {
        assertEquals(
                26.85,
                Main.convertTemperatureValue(300, "Kelvin", "Celsius"),
                0.001
        );
    }

    @Test
    void rejectsUnknownTemperatureUnit() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Main.convertTemperatureValue(10, "Unknown", "Celsius")
        );
    }
}