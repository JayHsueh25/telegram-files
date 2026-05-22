package telegram.files.http;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import telegram.files.EventEnum;
import telegram.files.repository.SettingKey;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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

    @Test
    void handleSettingsFailsWhenKeysAreBlank() {
        SettingsRoutes.SettingsStore settingsStore = mock(SettingsRoutes.SettingsStore.class);
        RoutingContext ctx = settingsContextWithKeys(" ");

        invokeHandler(new SettingsRoutes(settingsStore), "handleSettings", ctx);

        verify(ctx).fail(400);
        verifyNoInteractions(settingsStore);
    }

    @Test
    void handleSettingsReturnsExistingValuesAndDefaultsForMissingSettings() {
        SettingsRoutes.SettingsStore settingsStore = mock(SettingsRoutes.SettingsStore.class);
        RoutingContext ctx = settingsContextWithKeys("speedUnits,avgSpeedInterval");

        when(settingsStore.getByKeys(List.of("speedUnits", "avgSpeedInterval")))
                .thenReturn(Future.succeededFuture(List.of(
                        new SettingsRoutes.SettingValue(SettingKey.avgSpeedInterval.name(), "30"))));

        invokeHandler(new SettingsRoutes(settingsStore), "handleSettings", ctx);

        verify(settingsStore).getByKeys(List.of("speedUnits", "avgSpeedInterval"));
        verify(ctx).json(JsonObject.of(
                SettingKey.avgSpeedInterval.name(), "30",
                SettingKey.speedUnits.name(), SettingKey.speedUnits.defaultValue));
    }

    @Test
    void handleSettingsCreateFailsWhenBodyIsEmpty() {
        SettingsRoutes.SettingsStore settingsStore = mock(SettingsRoutes.SettingsStore.class);
        RoutingContext ctx = settingsCreateContextWithBody(new JsonObject());

        invokeHandler(new SettingsRoutes(settingsStore), "handleSettingsCreate", ctx);

        verify(ctx).fail(400);
        verifyNoInteractions(settingsStore);
    }

    @Test
    void handleSettingsCreateUpdatesSettingsPublishesEventsAndEndsResponse() {
        SettingsRoutes.SettingsStore settingsStore = mock(SettingsRoutes.SettingsStore.class);
        RoutingContext ctx = settingsCreateContextWithBody(JsonObject.of(SettingKey.speedUnits.name(), "bytes"));
        Vertx requestVertx = mock(Vertx.class);
        EventBus eventBus = mock(EventBus.class);

        when(ctx.vertx()).thenReturn(requestVertx);
        when(requestVertx.eventBus()).thenReturn(eventBus);
        when(settingsStore.createOrUpdate(SettingKey.speedUnits.name(), "bytes"))
                .thenReturn(Future.succeededFuture(
                        new SettingsRoutes.SettingValue(SettingKey.speedUnits.name(), "bytes")));

        invokeHandler(new SettingsRoutes(settingsStore), "handleSettingsCreate", ctx);

        verify(settingsStore).createOrUpdate(SettingKey.speedUnits.name(), "bytes");
        verify(eventBus).publish(EventEnum.SETTING_UPDATE.address(SettingKey.speedUnits.name()), "bytes");
        verify(ctx).end();
    }

    private static RoutingContext settingsContextWithKeys(String keys) {
        RoutingContext ctx = mock(RoutingContext.class);
        HttpServerRequest request = mock(HttpServerRequest.class);

        when(ctx.request()).thenReturn(request);
        when(request.getParam("keys")).thenReturn(keys);

        return ctx;
    }

    private static RoutingContext settingsCreateContextWithBody(JsonObject jsonBody) {
        RoutingContext ctx = mock(RoutingContext.class);
        RequestBody body = mock(RequestBody.class);

        when(ctx.body()).thenReturn(body);
        when(body.asJsonObject()).thenReturn(jsonBody);

        return ctx;
    }

    private static void invokeHandler(SettingsRoutes routes, String methodName, RoutingContext ctx) {
        try {
            Method method = SettingsRoutes.class.getDeclaredMethod(methodName, RoutingContext.class);
            method.setAccessible(true);
            method.invoke(routes, ctx);
        } catch (InvocationTargetException exception) {
            throw new AssertionError("Settings route handler threw an exception", exception.getCause());
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Settings route handler is not available for testing", exception);
        }
    }
}
