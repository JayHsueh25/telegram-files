package telegram.files.http;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

        long settingsRouteCount = router.getRoutes().stream()
                .filter(route -> route.getPath() != null)
                .filter(route -> route.getPath().startsWith("/settings"))
                .count();
        assertEquals(2, settingsRouteCount);
    }
}
