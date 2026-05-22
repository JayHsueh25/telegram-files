package telegram.files.http;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
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
        if (ctx.body() == null) {
            return JsonObject.of();
        }
        return ctx.body().asJsonObject();
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
                    ctx.json(result);
                })
                .onFailure(ctx::fail);
    }
}
