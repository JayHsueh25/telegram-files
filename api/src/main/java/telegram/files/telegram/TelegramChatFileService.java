package telegram.files.telegram;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ArrayUtil;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.drinkless.tdlib.TdApi;
import telegram.files.FileRecordRetriever;
import telegram.files.TdApiHelp;
import telegram.files.TelegramClient;
import telegram.files.TelegramConverter;
import telegram.files.repository.FileRecord;
import telegram.files.repository.TelegramRecord;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class TelegramChatFileService {

    public Future<JsonObject> getChatFiles(
            TelegramClient client,
            TelegramRecord telegramRecord,
            long chatId,
            Map<String, String> filter
    ) {
        boolean offline = Convert.toBool(filter.get("offline"), false);
        if (offline) {
            return FileRecordRetriever.getFiles(chatId, filter);
        } else {
            long messageThreadId = Convert.toLong(filter.get("messageThreadId"), 0L);
            TdApi.SearchChatMessages searchChatMessages = new TdApi.SearchChatMessages();
            searchChatMessages.chatId = chatId;
            searchChatMessages.query = filter.get("search");
            searchChatMessages.fromMessageId = Convert.toLong(filter.get("fromMessageId"), 0L);
            searchChatMessages.offset = Convert.toInt(filter.get("offset"), 0);
            searchChatMessages.limit = Convert.toInt(filter.get("limit"), 20);
            searchChatMessages.filter = TdApiHelp.getSearchMessagesFilter(filter.get("type"));
            searchChatMessages.topicId = messageThreadId > 0 ? new TdApi.MessageTopicThread(messageThreadId) : null;

            return (Objects.equals(filter.get("downloadStatus"), FileRecord.DownloadStatus.idle.name()) ?
                    this.getIdleChatFiles(client, searchChatMessages, 0) :
                    client.execute(searchChatMessages))
                    .compose(t -> TelegramConverter.convertFiles(telegramRecord.id(), t));
        }
    }

    private Future<TdApi.FoundChatMessages> getIdleChatFiles(TelegramClient client,
                                                             TdApi.SearchChatMessages searchChatMessages,
                                                             int seq) {
        if (seq != 0) {
            // Increase the limit and reduce the number of requests
            searchChatMessages.limit = 100;
        }
        return client.execute(searchChatMessages)
                .compose(foundChatMessages -> {
                    TdApi.Message[] messages = Stream.of(foundChatMessages.messages)
                            .filter(message ->
                                    TdApiHelp.getFileHandler(message)
                                            .map(TdApiHelp.FileHandler::getFile)
                                            .map(file -> file.local == null || (
                                                    !file.local.isDownloadingActive
                                                    && !file.local.isDownloadingCompleted
                                                    && file.local.downloadedSize == 0
                                            ))
                                            .orElse(false)
                            )
                            .toArray(TdApi.Message[]::new);
                    if (ArrayUtil.isEmpty(messages) && foundChatMessages.nextFromMessageId != 0) {
                        searchChatMessages.fromMessageId = foundChatMessages.nextFromMessageId;
                        return getIdleChatFiles(client, searchChatMessages, seq + 1);
                    } else {
                        foundChatMessages.messages = messages;
                        return Future.succeededFuture(foundChatMessages);
                    }
                });
    }

    public Future<JsonObject> getChatFilesCount(TelegramClient client, long chatId) {
        return Future.all(
                Stream.of(new TdApi.SearchMessagesFilterPhotoAndVideo(),
                                new TdApi.SearchMessagesFilterPhoto(),
                                new TdApi.SearchMessagesFilterVideo(),
                                new TdApi.SearchMessagesFilterAudio(),
                                new TdApi.SearchMessagesFilterDocument())
                        .map(filter -> client.execute(
                                                new TdApi.GetChatMessageCount(chatId,
                                                        null,
                                                        filter,
                                                        false)
                                        )
                                        .map(count -> new JsonObject()
                                                .put("type", TdApiHelp.getSearchMessagesFilterType(filter))
                                                .put("count", count.count)
                                        )
                        )
                        .toList()
        ).map(counts -> {
            JsonObject result = new JsonObject();
            counts.<JsonObject>list().forEach(count -> result.put(count.getString("type"), count.getInteger("count")));
            return result;
        });
    }
}
