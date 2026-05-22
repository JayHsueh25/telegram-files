package telegram.files.telegram;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.drinkless.tdlib.TdApi;
import org.jooq.lambda.tuple.Tuple;
import telegram.files.DataVerticle;
import telegram.files.EventEnum;
import telegram.files.EventPayload;
import telegram.files.TdApiHelp;
import telegram.files.TelegramClient;
import telegram.files.repository.FileRecord;
import telegram.files.repository.SettingAutoRecords;
import telegram.files.repository.SettingKey;
import telegram.files.repository.TelegramRecord;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class TelegramDownloadService {

    private static final Log log = LogFactory.get();

    @FunctionalInterface
    public interface FileDownloadStatusSync {
        Future<Void> sync(TdApi.File file, TdApi.Message message, TdApi.MessageThreadInfo messageThreadInfo);
    }

    public Future<FileRecord> startDownload(
            TelegramClient client,
            TelegramRecord telegramRecord,
            Long chatId,
            Long messageId,
            Integer fileId,
            FileDownloadStatusSync syncFileDownloadStatus,
            Consumer<EventPayload> sendEvent,
            Supplier<String> rootIdSupplier
    ) {
        return Future.all(
                        client.execute(new TdApi.GetFile(fileId)),
                        client.execute(new TdApi.GetMessage(chatId, messageId)),
                        client.execute(new TdApi.GetMessageThread(chatId, messageId), true)
                )
                .compose(results -> {
                    TdApi.File file = results.resultAt(0);
                    return DataVerticle.fileRepository.getByUniqueId(file.remote.uniqueId)
                            .map(fileRecord -> Tuple.tuple(file,
                                    results.<TdApi.Message>resultAt(1),
                                    results.<TdApi.MessageThreadInfo>resultAt(2),
                                    fileRecord
                            ));
                })
                .compose(results -> {
                    TdApi.File file = results.v1;
                    TdApi.Message message = results.v2;
                    TdApi.MessageThreadInfo messageThreadInfo = results.v3;
                    FileRecord dbFileRecord = results.v4;
                    if (file.local != null) {
                        if (file.local.isDownloadingCompleted) {
                            return syncFileDownloadStatus.sync(file, message, messageThreadInfo)
                                    .compose(_ -> DataVerticle.fileRepository.getByUniqueId(file.remote.uniqueId));
                        }
                        if (file.local.isDownloadingActive) {
                            return Future.failedFuture("File is downloading");
                        }
//                        return Future.failedFuture("Unknown file download status");
                    }
                    if (dbFileRecord != null && !dbFileRecord.isDownloadStatus(FileRecord.DownloadStatus.idle)) {
                        return Future.failedFuture("File is already downloading or completed");
                    }

                    TdApiHelp.FileHandler<? extends TdApi.MessageContent> fileHandler = TdApiHelp.getFileHandler(message)
                            .orElseThrow(() -> VertxException.noStackTrace("not support message type"));
                    FileRecord fileRecord = fileHandler.convertFileRecord(telegramRecord.id()).withThreadInfo(messageThreadInfo);
                    return DataVerticle.fileRepository.createIfNotExist(fileRecord)
                            .compose(created -> {
                                if (!created) {
                                    return DataVerticle.fileRepository.updateFileId(fileRecord.id(), fileRecord.uniqueId());
                                }
                                return Future.succeededFuture();
                            })
                            .compose(ignore -> client.execute(new TdApi.AddFileToDownloads(fileId, chatId, messageId, 32)))
                            .onSuccess(ignore -> {
                                sendEvent.accept(EventPayload.build(EventPayload.TYPE_FILE_STATUS, new JsonObject()
                                        .put("fileId", fileId)
                                        .put("uniqueId", fileRecord.uniqueId())
                                        .put("downloadStatus", FileRecord.DownloadStatus.downloading)
                                ));

                                downloadThumbnail(client, chatId, messageId, fileHandler.convertThumbnailRecord(telegramRecord.id()), rootIdSupplier);
                            })
                            .map(fileRecord);
                });
    }

    public Future<Boolean> downloadThumbnail(
            TelegramClient client,
            Long chatId,
            Long messageId,
            FileRecord thumbnailRecord,
            Supplier<String> rootIdSupplier
    ) {
        if (thumbnailRecord == null) {
            return Future.succeededFuture(false);
        }
        return DataVerticle.fileRepository.createIfNotExist(thumbnailRecord)
                .compose(created -> {
                    if (!created) {
                        return DataVerticle.fileRepository.updateFileId(thumbnailRecord.id(), thumbnailRecord.uniqueId());
                    }
                    return Future.succeededFuture();
                })
                .compose(ignore -> {
                    if (thumbnailRecord.isDownloadStatus(FileRecord.DownloadStatus.completed)) {
                        return Future.succeededFuture(false);
                    }
                    return client.execute(new TdApi.AddFileToDownloads(thumbnailRecord.id(), chatId, messageId, 32))
                            .map(true);
                })
                .onSuccess(download -> {
                    if (download) {
                        log.debug("[%s] Download thumbnail: %s".formatted(rootIdSupplier.get(), thumbnailRecord.uniqueId()));
                    }
                });
    }

    public Future<Void> cancelDownload(
            TelegramClient client,
            Integer fileId,
            Consumer<EventPayload> sendEvent
    ) {
        return client.execute(new TdApi.GetFile(fileId))
                .compose(file -> DataVerticle.fileRepository
                        .updateFileId(file.id, file.remote.uniqueId)
                        .map(file)
                )
                .compose(file -> {
                    if (file.local == null) {
                        return Future.failedFuture("File not started downloading");
                    }

                    return client.execute(new TdApi.CancelDownloadFile(fileId, false))
                            .map(file);
                })
                .compose(file -> client.execute(new TdApi.DeleteFile(fileId)).map(file))
                .compose(file -> DataVerticle.fileRepository.deleteByUniqueId(file.remote.uniqueId).map(file))
                .onSuccess(file ->
                        sendEvent.accept(EventPayload.build(EventPayload.TYPE_FILE_STATUS, new JsonObject()
                                .put("fileId", fileId)
                                .put("uniqueId", file.remote.uniqueId)
                                .put("downloadStatus", FileRecord.DownloadStatus.idle)
                        )))
                .mapEmpty();
    }

    public Future<Void> togglePauseDownload(
            TelegramClient client,
            Integer fileId,
            boolean isPaused,
            FileDownloadStatusSync syncFileDownloadStatus
    ) {
        return client.execute(new TdApi.GetFile(fileId))
                .compose(file -> DataVerticle.fileRepository
                        .updateFileId(file.id, file.remote.uniqueId)
                        .map(file)
                )
                .compose(file -> {
                    if (file.local == null) {
                        return Future.failedFuture("File not started downloading");
                    }
                    if (file.local.isDownloadingCompleted) {
                        return syncFileDownloadStatus.sync(file, null, null).mapEmpty();
                    }
                    if (isPaused && !file.local.isDownloadingActive) {
                        return Future.failedFuture("File is not downloading");
                    }
                    if (!isPaused && file.local.isDownloadingActive) {
                        return Future.failedFuture("File is downloading");
                    }
                    if (!isPaused && !file.local.canBeDeleted) {
                        // Maybe the file is not exist, so we need to redownload it
                        return DataVerticle.fileRepository.getByUniqueId(file.remote.uniqueId)
                                .compose(fileRecord ->
                                        client.execute(new TdApi.AddFileToDownloads(fileId, fileRecord.chatId(), fileRecord.messageId(), 32)))
                                .mapEmpty();
                    }

                    return client.execute(new TdApi.ToggleDownloadIsPaused(fileId, isPaused));
                })
                .mapEmpty();
    }

    public Future<Void> removeFile(
            TelegramClient client,
            Integer fileId,
            String uniqueId,
            Consumer<EventPayload> sendEvent,
            Supplier<String> rootIdSupplier
    ) {
        return client.execute(new TdApi.GetFile(fileId))
                .otherwise((TdApi.File) null)
                .compose(file -> DataVerticle.fileRepository
                        .getByUniqueId(uniqueId)
                        .map(fileRecord -> Tuple.tuple(file, fileRecord))
                )
                .compose(tuple2 -> {
                    TdApi.File file = tuple2.v1;
                    FileRecord fileRecord = tuple2.v2;
                    if (fileRecord == null) {
                        return Future.failedFuture("File not found");
                    }

                    if (fileRecord.isTransferStatus(FileRecord.TransferStatus.completed)) {
                        if (FileUtil.del(fileRecord.localPath())) {
                            log.debug("[%s] Remove file success: %s".formatted(rootIdSupplier.get(), fileRecord.localPath()));
                        }
                    }

                    if (file != null && file.local != null && StrUtil.isNotBlank(file.local.path)) {
                        return client.execute(new TdApi.DeleteFile(fileId))
                                .map(file);
                    } else if (!fileRecord.isTransferStatus(FileRecord.TransferStatus.completed)
                               && StrUtil.isNotBlank(fileRecord.localPath())) {
                        if (FileUtil.del(fileRecord.localPath())) {
                            log.debug("[%s] Remove file success: %s".formatted(rootIdSupplier.get(), fileRecord.localPath()));
                        }
                    }
                    return Future.succeededFuture(file);
                })
                .compose(file -> DataVerticle.fileRepository.deleteByUniqueId(uniqueId).map(file))
                .onSuccess(_ -> sendEvent.accept(EventPayload.build(EventPayload.TYPE_FILE_STATUS, new JsonObject()
                        .put("fileId", fileId)
                        .put("uniqueId", uniqueId)
                        .put("removed", true)
                )))
                .mapEmpty();
    }

    public Future<Void> updateAutoSettings(
            Vertx vertx,
            TelegramRecord telegramRecord,
            Long chatId,
            JsonObject params
    ) {
        return DataVerticle.settingRepository.<SettingAutoRecords>getByKey(SettingKey.automation)
                .compose(settingAutoRecords -> {
                    if (settingAutoRecords == null) {
                        settingAutoRecords = new SettingAutoRecords();
                    }
                    SettingAutoRecords.Automation automation = params.mapTo(SettingAutoRecords.Automation.class);
                    boolean hasEnabled = automation.preload.enabled
                                         || automation.download.enabled
                                         || automation.transfer.enabled;

                    if (settingAutoRecords.exists(telegramRecord.id(), chatId) && !hasEnabled) {
                        settingAutoRecords.remove(telegramRecord.id(), chatId);
                    } else {
                        if (!hasEnabled) {
                            return Future.succeededFuture();
                        }
                        automation.telegramId = telegramRecord.id();
                        automation.chatId = chatId;
                        settingAutoRecords.add(automation);
                    }

                    return DataVerticle.settingRepository.createOrUpdate(SettingKey.automation.name(), Json.encode(settingAutoRecords))
                            .onSuccess(r -> vertx.eventBus().publish(EventEnum.AUTO_DOWNLOAD_UPDATE.name(), r.value()));
                })
                .mapEmpty();
    }
}
