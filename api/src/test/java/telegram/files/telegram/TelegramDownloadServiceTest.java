package telegram.files.telegram;

import io.vertx.core.Future;
import org.drinkless.tdlib.TdApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import telegram.files.DataVerticle;
import telegram.files.TelegramClientGateway;
import telegram.files.repository.FileRecord;
import telegram.files.repository.FileRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TelegramDownloadServiceTest {

    private final FileRepository originalFileRepository = DataVerticle.fileRepository;

    @AfterEach
    void tearDown() {
        DataVerticle.fileRepository = originalFileRepository;
    }

    @Test
    void canBeConstructed() {
        Assertions.assertNotNull(new TelegramDownloadService());
    }

    @Test
    void downloadThumbnailReturnsFalseWhenThumbnailRecordIsMissing() {
        TelegramClientGateway client = mock(TelegramClientGateway.class);

        Boolean result = new TelegramDownloadService()
                .downloadThumbnail(client, 10L, 20L, null, () -> "root")
                .result();

        Assertions.assertFalse(result);
        verify(client, never()).execute(any(TdApi.AddFileToDownloads.class));
    }

    @Test
    void downloadThumbnailDoesNotStartDownloadForCompletedThumbnail() {
        DataVerticle.fileRepository = mock(FileRepository.class);
        FileRecord thumbnail = thumbnailRecord(FileRecord.DownloadStatus.completed);
        when(DataVerticle.fileRepository.createIfNotExist(thumbnail))
                .thenReturn(Future.succeededFuture(true));

        TelegramClientGateway client = mock(TelegramClientGateway.class);

        Boolean result = new TelegramDownloadService()
                .downloadThumbnail(client, 10L, 20L, thumbnail, () -> "root")
                .result();

        Assertions.assertFalse(result);
        verify(DataVerticle.fileRepository).createIfNotExist(thumbnail);
        verify(client, never()).execute(any(TdApi.AddFileToDownloads.class));
    }

    @Test
    void downloadThumbnailStartsDownloadAndRefreshesFileIdWhenRecordAlreadyExists() {
        DataVerticle.fileRepository = mock(FileRepository.class);
        FileRecord thumbnail = thumbnailRecord(FileRecord.DownloadStatus.idle);
        when(DataVerticle.fileRepository.createIfNotExist(thumbnail))
                .thenReturn(Future.succeededFuture(false));
        when(DataVerticle.fileRepository.updateFileId(thumbnail.id(), thumbnail.uniqueId()))
                .thenReturn(Future.succeededFuture());

        TelegramClientGateway client = mock(TelegramClientGateway.class);
        when(client.execute(any(TdApi.AddFileToDownloads.class)))
                .thenAnswer(invocation -> {
                    TdApi.AddFileToDownloads query = invocation.getArgument(0);
                    Assertions.assertEquals(thumbnail.id(), query.fileId);
                    Assertions.assertEquals(10L, query.chatId);
                    Assertions.assertEquals(20L, query.messageId);
                    Assertions.assertEquals(32, query.priority);
                    return Future.succeededFuture(new TdApi.File());
                });

        Boolean result = new TelegramDownloadService()
                .downloadThumbnail(client, 10L, 20L, thumbnail, () -> "root")
                .result();

        Assertions.assertTrue(result);
        verify(DataVerticle.fileRepository).createIfNotExist(thumbnail);
        verify(DataVerticle.fileRepository).updateFileId(thumbnail.id(), thumbnail.uniqueId());
        verify(client).execute(any(TdApi.AddFileToDownloads.class));
    }

    private FileRecord thumbnailRecord(FileRecord.DownloadStatus downloadStatus) {
        return new FileRecord(
                5,
                "thumbnail_unique_id",
                1,
                10,
                20,
                0,
                1,
                false,
                100,
                0,
                "thumbnail",
                "image/jpeg",
                "thumbnail.jpg",
                null,
                null,
                null,
                null,
                null,
                downloadStatus.name(),
                FileRecord.TransferStatus.idle.name(),
                0,
                null,
                null,
                0,
                0,
                0
        );
    }
}
