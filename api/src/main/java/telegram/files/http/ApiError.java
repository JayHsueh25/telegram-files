package telegram.files.http;

import io.vertx.core.json.JsonObject;

public record ApiError(String error) {

    public static ApiError of(String message) {
        return new ApiError(message);
    }

    public JsonObject toJson() {
        return JsonObject.of("error", error);
    }
}
