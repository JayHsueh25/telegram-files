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

import java.util.Arrays;
import java.util.List;

public class SettingsRoutes {

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
                        .map(setting -> DataVerticle.settingRepository.createOrUpdate(setting.getKey(),
                                Convert.toStr(setting.getValue(), "")))
                        .toList())
                .map(CompositeFuture::<SettingRecord>list)
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
        DataVerticle.settingRepository
                .getByKeys(keys)
                .onSuccess(settings -> {
                    JsonObject object = new JsonObject();
                    for (SettingRecord record : settings) {
                        object.put(record.key(), record.value());
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
}
