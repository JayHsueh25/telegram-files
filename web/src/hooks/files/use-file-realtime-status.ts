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
    downloadStatus?: DownloadStatus;
    localPath?: string;
    completionDate?: number;
    downloadedSize?: number;
    transferStatus?: TransferStatus;
    thumbnailFile?: Thumbnail;
    removed?: boolean;
  }
>;

type FileStatusMessage = {
  type?: unknown;
  data: FileStatusPayload;
};

type FileStatusPayload = {
  fileId: number;
  uniqueId: string;
  downloadStatus?: DownloadStatus;
  localPath?: string;
  completionDate?: number;
  downloadedSize?: number;
  transferStatus?: TransferStatus;
  thumbnailFile?: Thumbnail;
  removed?: boolean;
};

const DOWNLOAD_STATUSES = [
  "idle",
  "downloading",
  "paused",
  "completed",
  "error",
] satisfies DownloadStatus[];

const TRANSFER_STATUSES = [
  "idle",
  "transferring",
  "completed",
  "error",
] satisfies TransferStatus[];

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function isDownloadStatus(value: unknown): value is DownloadStatus {
  return (
    typeof value === "string" &&
    DOWNLOAD_STATUSES.includes(value as DownloadStatus)
  );
}

function isTransferStatus(value: unknown): value is TransferStatus {
  return (
    typeof value === "string" &&
    TRANSFER_STATUSES.includes(value as TransferStatus)
  );
}

function isFileStatusPayload(data: unknown): data is FileStatusPayload {
  if (!isObject(data)) {
    return false;
  }

  if (typeof data.uniqueId !== "string" || data.uniqueId.trim() === "") {
    return false;
  }

  if (typeof data.fileId !== "number") {
    return false;
  }

  if (
    "downloadStatus" in data &&
    data.downloadStatus !== undefined &&
    !isDownloadStatus(data.downloadStatus)
  ) {
    return false;
  }

  if (
    "downloadedSize" in data &&
    data.downloadedSize !== undefined &&
    typeof data.downloadedSize !== "number"
  ) {
    return false;
  }

  if (
    "localPath" in data &&
    data.localPath !== undefined &&
    typeof data.localPath !== "string"
  ) {
    return false;
  }

  if (
    "completionDate" in data &&
    data.completionDate !== undefined &&
    typeof data.completionDate !== "number"
  ) {
    return false;
  }

  if (
    "removed" in data &&
    data.removed !== undefined &&
    typeof data.removed !== "boolean"
  ) {
    return false;
  }

  if (
    "transferStatus" in data &&
    data.transferStatus !== undefined &&
    !isTransferStatus(data.transferStatus)
  ) {
    return false;
  }

  return true;
}

function isFileStatusMessage(
  lastJsonMessage: unknown,
): lastJsonMessage is FileStatusMessage {
  return (
    isObject(lastJsonMessage) &&
    "type" in lastJsonMessage &&
    lastJsonMessage.type === WebSocketMessageType.FILE_STATUS &&
    isFileStatusPayload(lastJsonMessage.data)
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
