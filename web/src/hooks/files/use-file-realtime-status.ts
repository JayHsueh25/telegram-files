import { useEffect, useState } from "react";
import {
  type DownloadStatus,
  type TelegramFile,
  type Thumbnail,
  type TransferStatus,
} from "@/lib/types";
import { WebSocketMessageType } from "@/lib/websocket-types";

export type LatestFileStatus = Record<
  string,
  {
    fileId: number;
    downloadStatus: DownloadStatus;
    localPath?: string;
    completionDate?: number;
    downloadedSize: number;
    transferStatus?: TransferStatus;
    thumbnailFile?: Thumbnail;
    removed?: boolean;
  }
>;

type FileStatusMessage = {
  type?: unknown;
  data?: FileStatusPayload;
};

type FileStatusPayload = {
  fileId: number;
  uniqueId: string;
  downloadStatus: DownloadStatus;
  localPath: string;
  completionDate: number;
  downloadedSize: number;
  transferStatus?: TransferStatus;
  thumbnailFile?: Thumbnail;
  removed?: boolean;
};

function isFileStatusMessage(
  lastJsonMessage: unknown,
): lastJsonMessage is FileStatusMessage {
  return (
    typeof lastJsonMessage === "object" &&
    lastJsonMessage !== null &&
    "type" in lastJsonMessage &&
    lastJsonMessage.type === WebSocketMessageType.FILE_STATUS
  );
}

export function useFileRealtimeStatus(
  lastJsonMessage: unknown,
): LatestFileStatus {
  const [latestFilesStatus, setLatestFileStatus] =
    useState<LatestFileStatus>({});

  useEffect(() => {
    if (!isFileStatusMessage(lastJsonMessage)) {
      return;
    }

    const data = lastJsonMessage.data;
    if (!data) {
      return;
    }

    if (data.removed) {
      setLatestFileStatus((prev) => ({
        ...prev,
        [data.uniqueId]: {
          fileId: data.fileId,
          downloadStatus: "idle",
          localPath: undefined,
          completionDate: undefined,
          downloadedSize: 0,
          transferStatus: "idle",
          removed: true,
        },
      }));
      return;
    }

    setLatestFileStatus((prev) => ({
      ...prev,
      [data.uniqueId]: {
        fileId: data.fileId,
        downloadStatus:
          data.downloadStatus ?? prev[data.uniqueId]?.downloadStatus,
        localPath: data.localPath ?? prev[data.uniqueId]?.localPath,
        completionDate:
          data.completionDate ?? prev[data.uniqueId]?.completionDate,
        downloadedSize:
          data.downloadedSize ?? prev[data.uniqueId]?.downloadedSize,
        transferStatus:
          data.transferStatus ?? prev[data.uniqueId]?.transferStatus,
        thumbnailFile: data.thumbnailFile ?? prev[data.uniqueId]?.thumbnailFile,
      },
    }));
  }, [lastJsonMessage]);

  return latestFilesStatus;
}

export function mergeRealtimeStatus(
  file: TelegramFile,
  latestFilesStatus: LatestFileStatus,
): TelegramFile | null {
  const latest = latestFilesStatus[file.uniqueId];

  if (file.originalDeleted && latest?.removed) {
    return null;
  }

  return {
    ...file,
    id: latest?.fileId ?? file.id,
    downloadStatus: latest?.downloadStatus ?? file.downloadStatus,
    localPath: latest?.localPath ?? file.localPath,
    completionDate: latest?.completionDate ?? file.completionDate,
    downloadedSize: latest?.downloadedSize ?? file.downloadedSize,
    transferStatus: latest?.transferStatus ?? file.transferStatus,
    thumbnailFile: latest?.thumbnailFile ?? file.thumbnailFile,
  };
}
