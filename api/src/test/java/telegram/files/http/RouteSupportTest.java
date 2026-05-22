package telegram.files.http;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RouteSupportTest {

    @Test
    void parseRequiredLongRejectsBlankValue() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> RouteSupport.parseRequiredLong("", "telegramId"));

        assertEquals("telegramId is required", exception.getMessage());
    }

    @Test
    void apiErrorSerializesMessageToErrorField() {
        assertEquals("Invalid request", ApiError.of("Invalid request").toJson().getString("error"));
    }

    @Test
    void parseRequiredLongRejectsNonNumericValue() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> RouteSupport.parseRequiredLong("abc", "telegramId"));

        assertEquals("telegramId must be a number", exception.getMessage());
        assertNotNull(exception.getCause());
    }
}
