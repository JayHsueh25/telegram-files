package telegram.files.http;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileRoutesTest {

    private static final List<RouteSpec> FILE_ROUTES = List.of(
            new RouteSpec("/:telegramId/file/:uniqueId", HttpMethod.GET),
            new RouteSpec("/:telegramId/file/start-download", HttpMethod.POST),
            new RouteSpec("/:telegramId/file/cancel-download", HttpMethod.POST),
            new RouteSpec("/:telegramId/file/toggle-pause-download", HttpMethod.POST),
            new RouteSpec("/:telegramId/file/remove", HttpMethod.POST),
            new RouteSpec("/:telegramId/file/update-auto-settings", HttpMethod.POST),
            new RouteSpec("/files/count", HttpMethod.GET),
            new RouteSpec("/files", HttpMethod.GET),
            new RouteSpec("/files/start-download-multiple", HttpMethod.POST),
            new RouteSpec("/files/cancel-download-multiple", HttpMethod.POST),
            new RouteSpec("/files/toggle-pause-download-multiple", HttpMethod.POST),
            new RouteSpec("/files/remove-multiple", HttpMethod.POST),
            new RouteSpec("/files/update-tags", HttpMethod.POST),
            new RouteSpec("/file/:uniqueId/update-tags", HttpMethod.POST)
    );

    private final Vertx vertx = Vertx.vertx();

    @AfterEach
    void closeVertx() {
        vertx.close();
    }

    @Test
    void mountRegistersEveryFileRouteWithExpectedMethod() {
        Router router = Router.router(vertx);

        fileRoutes().mount(router);

        assertEquals(FILE_ROUTES.size(), router.getRoutes().size());
        for (RouteSpec routeSpec : FILE_ROUTES) {
            assertTrue(router.getRoutes().stream()
                            .anyMatch(route -> routeSpec.path().equals(route.getPath())
                                    && route.methods().contains(routeSpec.method())),
                    "Expected %s %s to be registered".formatted(routeSpec.method(), routeSpec.path()));
        }
    }

    private static FileRoutes fileRoutes() {
        return new FileRoutes(
                FileRoutesTest::dummyHandler,
                FileRoutesTest::dummyHandler,
                FileRoutesTest::dummyHandler,
                FileRoutesTest::dummyHandler,
                FileRoutesTest::dummyHandler,
                FileRoutesTest::dummyHandler,
                FileRoutesTest::dummyHandler,
                FileRoutesTest::dummyHandler,
                FileRoutesTest::dummyHandler,
                FileRoutesTest::dummyHandler,
                FileRoutesTest::dummyHandler,
                FileRoutesTest::dummyHandler,
                FileRoutesTest::dummyHandler,
                FileRoutesTest::dummyHandler
        );
    }

    private static void dummyHandler(RoutingContext ctx) {
        // The route registration test only needs a stable handler reference.
    }

    private record RouteSpec(String path, HttpMethod method) {
    }
}
