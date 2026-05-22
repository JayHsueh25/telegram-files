package telegram.files.http;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ApiErrorTest {

    @Test
    void includesCodeAndMessage() {
        ApiError error = ApiError.of("VALIDATION_ERROR", "Invalid request");

        Assertions.assertEquals("VALIDATION_ERROR", error.toJson().getString("code"));
        Assertions.assertEquals("Invalid request", error.toJson().getString("error"));
    }
}
