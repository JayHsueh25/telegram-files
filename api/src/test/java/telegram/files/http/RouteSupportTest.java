package telegram.files.http;

import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    @Test
    void bodyReturnsEmptyJsonObjectWhenRequestBodyIsNull() {
        RoutingContext ctx = mock(RoutingContext.class);

        when(ctx.body()).thenReturn(null);

        assertTrue(RouteSupport.body(ctx).isEmpty());
    }

    @Test
    void bodyReturnsJsonBody() {
        RoutingContext ctx = mock(RoutingContext.class);
        RequestBody body = mock(RequestBody.class);
        JsonObject jsonBody = JsonObject.of("telegramId", 123L);

        when(ctx.body()).thenReturn(body);
        when(body.asJsonObject()).thenReturn(jsonBody);

        assertEquals(jsonBody, RouteSupport.body(ctx));
    }

    @Test
    void jsonWritesSuccessfulResult() {
        RoutingContext ctx = routingContextWithOpenResponse();
        JsonObject result = JsonObject.of("ok", true);

        RouteSupport.json(ctx, Future.succeededFuture(result));

        verify(ctx).json(result);
    }

    @Test
    void jsonEndsResponseWhenSuccessfulResultIsNull() {
        RoutingContext ctx = routingContextWithOpenResponse();

        RouteSupport.json(ctx, Future.succeededFuture(null));

        verify(ctx).end();
    }

    @Test
    void jsonFailsContextWhenFutureFailsBeforeResponseEnds() {
        RoutingContext ctx = routingContextWithOpenResponse();
        RuntimeException error = new RuntimeException("boom");

        RouteSupport.json(ctx, Future.failedFuture(error));

        verify(ctx).fail(error);
    }

    @Test
    void jsonDoesNotFailContextWhenFutureFailsAfterResponseEnds() {
        RoutingContext ctx = routingContextWithEndedResponse();
        RuntimeException error = new RuntimeException("boom");

        RouteSupport.json(ctx, Future.failedFuture(error));

        verify(ctx, never()).fail(error);
    }

    @Test
    void jsonFailsContextWhenWritingSuccessfulResultThrowsBeforeResponseEnds() {
        RoutingContext ctx = routingContextWithOpenResponse();
        JsonObject result = JsonObject.of("ok", true);
        RuntimeException error = new RuntimeException("encode failed");

        when(ctx.json(result)).thenThrow(error);

        RouteSupport.json(ctx, Future.succeededFuture(result));

        verify(ctx).fail(error);
    }

    private static RoutingContext routingContextWithOpenResponse() {
        return routingContextWithResponseEnded(false);
    }

    private static RoutingContext routingContextWithEndedResponse() {
        return routingContextWithResponseEnded(true);
    }

    private static RoutingContext routingContextWithResponseEnded(boolean ended) {
        RoutingContext ctx = mock(RoutingContext.class);
        HttpServerResponse response = mock(HttpServerResponse.class);

        when(ctx.response()).thenReturn(response);
        when(response.ended()).thenReturn(ended);

        return ctx;
    }
}
