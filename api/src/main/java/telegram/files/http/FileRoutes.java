package telegram.files.http;

import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class FileRoutes {

    private final Handler<RoutingContext> filePreviewHandler;
    private final Handler<RoutingContext> fileStartDownloadHandler;
    private final Handler<RoutingContext> fileCancelDownloadHandler;
    private final Handler<RoutingContext> fileTogglePauseDownloadHandler;
    private final Handler<RoutingContext> fileRemoveHandler;
    private final Handler<RoutingContext> autoSettingsUpdateHandler;
    private final Handler<RoutingContext> filesCountHandler;
    private final Handler<RoutingContext> filesHandler;
    private final Handler<RoutingContext> fileStartDownloadMultipleHandler;
    private final Handler<RoutingContext> fileCancelDownloadMultipleHandler;
    private final Handler<RoutingContext> fileTogglePauseDownloadMultipleHandler;
    private final Handler<RoutingContext> fileRemoveMultipleHandler;
    private final Handler<RoutingContext> fileTagsUpdateMultipleHandler;
    private final Handler<RoutingContext> fileTagsUpdateHandler;

    public FileRoutes(Handler<RoutingContext> filePreviewHandler,
                      Handler<RoutingContext> fileStartDownloadHandler,
                      Handler<RoutingContext> fileCancelDownloadHandler,
                      Handler<RoutingContext> fileTogglePauseDownloadHandler,
                      Handler<RoutingContext> fileRemoveHandler,
                      Handler<RoutingContext> autoSettingsUpdateHandler,
                      Handler<RoutingContext> filesCountHandler,
                      Handler<RoutingContext> filesHandler,
                      Handler<RoutingContext> fileStartDownloadMultipleHandler,
                      Handler<RoutingContext> fileCancelDownloadMultipleHandler,
                      Handler<RoutingContext> fileTogglePauseDownloadMultipleHandler,
                      Handler<RoutingContext> fileRemoveMultipleHandler,
                      Handler<RoutingContext> fileTagsUpdateMultipleHandler,
                      Handler<RoutingContext> fileTagsUpdateHandler) {
        this.filePreviewHandler = filePreviewHandler;
        this.fileStartDownloadHandler = fileStartDownloadHandler;
        this.fileCancelDownloadHandler = fileCancelDownloadHandler;
        this.fileTogglePauseDownloadHandler = fileTogglePauseDownloadHandler;
        this.fileRemoveHandler = fileRemoveHandler;
        this.autoSettingsUpdateHandler = autoSettingsUpdateHandler;
        this.filesCountHandler = filesCountHandler;
        this.filesHandler = filesHandler;
        this.fileStartDownloadMultipleHandler = fileStartDownloadMultipleHandler;
        this.fileCancelDownloadMultipleHandler = fileCancelDownloadMultipleHandler;
        this.fileTogglePauseDownloadMultipleHandler = fileTogglePauseDownloadMultipleHandler;
        this.fileRemoveMultipleHandler = fileRemoveMultipleHandler;
        this.fileTagsUpdateMultipleHandler = fileTagsUpdateMultipleHandler;
        this.fileTagsUpdateHandler = fileTagsUpdateHandler;
    }

    public void mount(Router router) {
        router.get("/:telegramId/file/:uniqueId").handler(filePreviewHandler);
        router.post("/:telegramId/file/start-download").handler(fileStartDownloadHandler);
        router.post("/:telegramId/file/cancel-download").handler(fileCancelDownloadHandler);
        router.post("/:telegramId/file/toggle-pause-download").handler(fileTogglePauseDownloadHandler);
        router.post("/:telegramId/file/remove").handler(fileRemoveHandler);
        router.post("/:telegramId/file/update-auto-settings").handler(autoSettingsUpdateHandler);
        router.get("/files/count").handler(filesCountHandler);
        router.get("/files").handler(filesHandler);
        router.post("/files/start-download-multiple").handler(fileStartDownloadMultipleHandler);
        router.post("/files/cancel-download-multiple").handler(fileCancelDownloadMultipleHandler);
        router.post("/files/toggle-pause-download-multiple").handler(fileTogglePauseDownloadMultipleHandler);
        router.post("/files/remove-multiple").handler(fileRemoveMultipleHandler);
        router.post("/files/update-tags").handler(fileTagsUpdateMultipleHandler);
        router.post("/file/:uniqueId/update-tags").handler(fileTagsUpdateHandler);
    }
}
