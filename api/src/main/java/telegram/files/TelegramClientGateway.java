package telegram.files;

import io.vertx.core.Future;
import org.drinkless.tdlib.TdApi;

public interface TelegramClientGateway {

    <R extends TdApi.Object> Future<R> execute(TdApi.Function<R> method);

    <R extends TdApi.Object> Future<R> execute(TdApi.Function<R> method, boolean ignoreException);
}
