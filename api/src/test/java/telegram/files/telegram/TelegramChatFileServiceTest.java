package telegram.files.telegram;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.drinkless.tdlib.TdApi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import telegram.files.TelegramClientGateway;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TelegramChatFileServiceTest {

    @Test
    void canBeConstructedWithDependencies() {
        TelegramChatFileService service = new TelegramChatFileService();

        Assertions.assertNotNull(service);
    }

    @Test
    void getChatFilesCountAggregatesCountsByFileType() {
        TelegramClientGateway client = mock(TelegramClientGateway.class);
        when(client.execute(any(TdApi.GetChatMessageCount.class))).thenAnswer(invocation -> {
            TdApi.GetChatMessageCount query = invocation.getArgument(0);
            int count = switch (query.filter.getConstructor()) {
                case TdApi.SearchMessagesFilterPhotoAndVideo.CONSTRUCTOR -> 11;
                case TdApi.SearchMessagesFilterPhoto.CONSTRUCTOR -> 7;
                case TdApi.SearchMessagesFilterVideo.CONSTRUCTOR -> 5;
                case TdApi.SearchMessagesFilterAudio.CONSTRUCTOR -> 3;
                case TdApi.SearchMessagesFilterDocument.CONSTRUCTOR -> 2;
                default -> 0;
            };
            return Future.succeededFuture(new TdApi.Count(count));
        });

        JsonObject result = new TelegramChatFileService()
                .getChatFilesCount(client, 123L)
                .result();

        Assertions.assertEquals(11, result.getInteger("media"));
        Assertions.assertEquals(7, result.getInteger("photo"));
        Assertions.assertEquals(5, result.getInteger("video"));
        Assertions.assertEquals(3, result.getInteger("audio"));
        Assertions.assertEquals(2, result.getInteger("file"));
    }
}
