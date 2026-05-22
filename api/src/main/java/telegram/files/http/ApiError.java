package telegram.files.http;

import io.vertx.core.json.JsonObject;

public record ApiError(String code, String error) {

    public static ApiError of(String message) {
        return new ApiError("INTERNAL_ERROR", message);
    }

    public static ApiError of(String code, String message) {
        return new ApiError(code, message);
    }

    public JsonObject toJson() {
        return JsonObject.of("code", code, "error", error);
    }
}
