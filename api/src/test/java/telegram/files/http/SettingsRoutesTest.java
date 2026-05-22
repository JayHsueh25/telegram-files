package telegram.files.http;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsRoutesTest {

    private final Vertx vertx = Vertx.vertx();

    @AfterEach
    void closeVertx() {
        vertx.close();
    }

    @Test
    void mountRegistersSettingsRoutes() {
        Router router = Router.router(vertx);

        new SettingsRoutes().mount(router);

        assertTrue(router.getRoutes().stream()
                .anyMatch(route -> "/settings".equals(route.getPath())
                        && route.methods().contains(HttpMethod.GET)));
        assertTrue(router.getRoutes().stream()
                .anyMatch(route -> "/settings/create".equals(route.getPath())
                        && route.methods().contains(HttpMethod.POST)));
    }
}
