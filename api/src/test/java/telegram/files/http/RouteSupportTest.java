package telegram.files.http;

import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    void bodyReturnsEmptyJsonObjectWhenJsonBodyIsNull() {
        RoutingContext ctx = mock(RoutingContext.class);
        RequestBody body = mock(RequestBody.class);

        when(ctx.body()).thenReturn(body);
        when(body.asJsonObject()).thenReturn(null);

        assertTrue(RouteSupport.body(ctx).isEmpty());
    }
}
