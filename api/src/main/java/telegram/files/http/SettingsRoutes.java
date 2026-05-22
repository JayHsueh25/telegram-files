package telegram.files.http;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import telegram.files.DataVerticle;
import telegram.files.EventEnum;
import telegram.files.repository.SettingKey;
import telegram.files.repository.SettingRecord;
import telegram.files.repository.SettingRepository;

import java.util.Arrays;
import java.util.List;

public class SettingsRoutes {

    private final SettingsStore settingsStore;

    public SettingsRoutes() {
        this(new RepositorySettingsStore());
    }

    SettingsRoutes(SettingsStore settingsStore) {
        this.settingsStore = settingsStore;
    }

    public void mount(Router router) {
        router.get("/settings").handler(this::handleSettings);
        router.post("/settings/create").handler(this::handleSettingsCreate);
    }

    private void handleSettingsCreate(RoutingContext ctx) {
        JsonObject object = RouteSupport.body(ctx);
        if (object.isEmpty()) {
            ctx.fail(400);
            return;
        }

        Future.all(object.stream()
                        .map(setting -> settingsStore.createOrUpdate(setting.getKey(),
                                Convert.toStr(setting.getValue(), "")))
                        .toList())
                .map(CompositeFuture::<SettingValue>list)
                .onSuccess(records -> {
                    records.forEach(record ->
                            ctx.vertx().eventBus().publish(EventEnum.SETTING_UPDATE.address(record.key()), record.value()));
                    ctx.end();
                })
                .onFailure(ctx::fail);
    }

    private void handleSettings(RoutingContext ctx) {
        String keysStr = ctx.request().getParam("keys");
        if (StrUtil.isBlank(keysStr)) {
            ctx.fail(400);
            return;
        }
        List<String> keys = Arrays.asList(keysStr.split(","));
        settingsStore.getByKeys(keys)
                .onSuccess(settings -> {
                    JsonObject object = new JsonObject();
                    for (SettingValue setting : settings) {
                        object.put(setting.key(), setting.value());
                    }
                    for (String key : keys) {
                        if (object.containsKey(key)) {
                            continue;
                        }
                        object.put(key, SettingKey.valueOf(key).defaultValue);
                    }
                    ctx.json(object);
                })
                .onFailure(ctx::fail);
    }

    interface SettingsStore {
        Future<SettingValue> createOrUpdate(String key, String value);

        Future<List<SettingValue>> getByKeys(List<String> keys);
    }

    record SettingValue(String key, Object value) {
    }

    private static class RepositorySettingsStore implements SettingsStore {

        @Override
        public Future<SettingValue> createOrUpdate(String key, String value) {
            return settingRepository()
                    .createOrUpdate(key, value)
                    .map(RepositorySettingsStore::toSettingValue);
        }

        @Override
        public Future<List<SettingValue>> getByKeys(List<String> keys) {
            return settingRepository()
                    .getByKeys(keys)
                    .map(records -> records.stream()
                            .map(RepositorySettingsStore::toSettingValue)
                            .toList());
        }

        private static SettingRepository settingRepository() {
            return DataVerticle.settingRepository;
        }

        private static SettingValue toSettingValue(SettingRecord record) {
            return new SettingValue(record.key(), record.value());
        }
    }
}
