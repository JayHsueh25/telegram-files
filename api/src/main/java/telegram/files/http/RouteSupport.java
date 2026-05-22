package telegram.files.http;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;

public final class RouteSupport {

    private RouteSupport() {
    }

    public static long parseRequiredLong(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException cause) {
            throw new IllegalArgumentException(fieldName + " must be a number", cause);
        }
    }

    public static JsonObject body(RoutingContext ctx) {
        RequestBody requestBody = ctx.body();
        if (requestBody == null) {
            return JsonObject.of();
        }
        JsonObject jsonBody = requestBody.asJsonObject();
        if (jsonBody == null) {
            return JsonObject.of();
        }
        return jsonBody;
    }

    public static <T> void json(RoutingContext ctx, Future<T> future) {
        future.onSuccess(result -> {
                    if (ctx.response().ended()) {
                        return;
                    }
                    if (result == null) {
                        ctx.end();
                        return;
                    }
                    try {
                        ctx.json(result);
                    } catch (Throwable error) {
                        if (!ctx.response().ended()) {
                            ctx.fail(error);
                        }
                    }
                })
                .onFailure(error -> {
                    if (!ctx.response().ended()) {
                        ctx.fail(error);
                    }
                });
    }
}
