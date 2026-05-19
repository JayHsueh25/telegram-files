package telegram.files;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import telegram.files.repository.SettingKey;
import telegram.files.repository.SettingRecord;
import telegram.files.repository.SettingRepository;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@ExtendWith(VertxExtension.class)
public class HttpVerticleAuthTest {

    private final SettingRepository settingRepository = new InMemorySettingRepository();

    private HttpClient httpClient;

    private HttpServer httpServer;

    private String baseUrl;

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext testContext) {
        DataVerticle.settingRepository = settingRepository;
        httpClient = HttpClient.newBuilder()
                .cookieHandler(new CookieManager())
                .build();

        AdminAuthService authService = new AdminAuthService();
        HttpVerticle httpVerticle = new HttpVerticle();
        httpVerticle.init(vertx, vertx.getOrCreateContext());

        authService.ensureInitialized()
                .compose(_ -> vertx.createHttpServer()
                        .requestHandler(httpVerticle.initRouter())
                        .listen(0))
                .onComplete(testContext.succeeding(server -> testContext.verify(() -> {
                    httpServer = server;
                    baseUrl = "http://localhost:" + server.actualPort();
                    testContext.completeNow();
                })));
    }

    @AfterEach
    void tearDown(VertxTestContext testContext) {
        DataVerticle.settingRepository = null;
        if (httpServer == null) {
            testContext.completeNow();
            return;
        }
        httpServer.close().onComplete(testContext.succeedingThenComplete());
    }

    @Test
    @DisplayName("未登录访问受保护接口返回 401")
    void rejectProtectedRouteWithoutSession(VertxTestContext testContext) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/files"))
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .whenComplete((response, error) -> testContext.verify(() -> {
                    Assertions.assertNull(error);
                    Assertions.assertEquals(401, response.statusCode());
                    Assertions.assertTrue(response.body().contains("Unauthorized"));
                    testContext.completeNow();
                }));
    }

    @Test
    @DisplayName("登录成功后可以读取会话状态并退出登录")
    void loginSessionAndLogoutFlow(VertxTestContext testContext) {
        String loginBody = """
                {"username":"admin","password":"admin"}
                """;

        HttpRequest loginRequest = HttpRequest.newBuilder(URI.create(baseUrl + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(loginBody))
                .build();

        httpClient.sendAsync(loginRequest, HttpResponse.BodyHandlers.ofString())
                .thenCompose(loginResponse -> {
                    String sessionCookie = loginResponse.headers()
                            .firstValue("set-cookie")
                            .map(cookie -> cookie.split(";", 2)[0])
                            .orElseThrow(() -> new AssertionError("Missing session cookie"));
                    testContext.verify(() -> {
                        Assertions.assertEquals(200, loginResponse.statusCode());
                        JsonObject loginJson = new JsonObject(loginResponse.body());
                        Assertions.assertTrue(loginJson.getBoolean("authenticated"));
                        Assertions.assertEquals("admin", loginJson.getString("username"));
                    });
                    HttpRequest sessionRequest = HttpRequest.newBuilder(URI.create(baseUrl + "/auth/session"))
                            .header("Cookie", sessionCookie)
                            .GET()
                            .build();
                    return httpClient.sendAsync(sessionRequest, HttpResponse.BodyHandlers.ofString())
                            .thenApply(response -> new SessionStep(response, sessionCookie));
                })
                .thenCompose(step -> {
                    testContext.verify(() -> {
                        Assertions.assertEquals(200, step.response.statusCode());
                        JsonObject sessionJson = new JsonObject(step.response.body());
                        Assertions.assertTrue(sessionJson.getBoolean("authenticated"));
                        Assertions.assertEquals("admin", sessionJson.getString("username"));
                    });
                    HttpRequest logoutRequest = HttpRequest.newBuilder(URI.create(baseUrl + "/auth/logout"))
                            .header("Cookie", step.sessionCookie)
                            .POST(HttpRequest.BodyPublishers.noBody())
                            .build();
                    return httpClient.sendAsync(logoutRequest, HttpResponse.BodyHandlers.ofString())
                            .thenApply(response -> new SessionStep(response, step.sessionCookie));
                })
                .thenCompose(step -> {
                    testContext.verify(() -> Assertions.assertEquals(200, step.response.statusCode()));
                    HttpRequest sessionRequest = HttpRequest.newBuilder(URI.create(baseUrl + "/auth/session"))
                            .header("Cookie", step.sessionCookie)
                            .GET()
                            .build();
                    return httpClient.sendAsync(sessionRequest, HttpResponse.BodyHandlers.ofString());
                })
                .whenComplete((sessionResponse, error) -> testContext.verify(() -> {
                    Assertions.assertNull(error);
                    Assertions.assertEquals(200, sessionResponse.statusCode());
                    JsonObject sessionJson = new JsonObject(sessionResponse.body());
                    Assertions.assertFalse(sessionJson.getBoolean("authenticated"));
                    testContext.completeNow();
                }));
    }

    private static class InMemorySettingRepository implements SettingRepository {

        private final Map<String, String> settings = new HashMap<>();

        @Override
        public Future<SettingRecord> createOrUpdate(String key, String value) {
            settings.put(key, value);
            return Future.succeededFuture(new SettingRecord(key, value));
        }

        @Override
        public Future<List<SettingRecord>> getByKeys(List<String> keys) {
            return Future.succeededFuture(keys.stream()
                    .filter(settings::containsKey)
                    .map(key -> new SettingRecord(key, settings.get(key)))
                    .toList());
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> Future<T> getByKey(SettingKey key) {
            String value = settings.get(key.name());
            if (value == null) {
                return Future.succeededFuture(key.defaultValue == null ? null : (T) key.defaultValue);
            }
            return Future.succeededFuture((T) key.converter.apply(value));
        }
    }

    private record SessionStep(HttpResponse<String> response, String sessionCookie) {
    }
}
