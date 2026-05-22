import { type FileFilter, type TelegramFile } from "@/lib/types";

type FileQueryPage = {
  files: TelegramFile[];
  nextFromMessageId: number;
};

type BuildFileQueryKeyOptions = {
  accountId: string;
  chatId: string;
  filters: FileFilter;
  page: number;
  previousPageData?: FileQueryPage | null;
  messageThreadId?: number;
  link?: string;
};

export function buildFileQueryKey({
  accountId,
  chatId,
  filters,
  page,
  previousPageData,
  messageThreadId,
  link,
}: BuildFileQueryKeyOptions) {
  const url =
    accountId === "-1" && chatId === "-1"
      ? "/files"
      : `/telegram/${accountId}/chat/${chatId}/files`;
  const params = new URLSearchParams({
    ...(filters.search && {
      search: encodeURIComponent(filters.search),
    }),
    ...(filters.type && { type: filters.type }),
    ...(filters.downloadStatus && { downloadStatus: filters.downloadStatus }),
    ...(filters.transferStatus && { transferStatus: filters.transferStatus }),
    ...(filters.offline && { offline: "true" }),
    ...(filters.tags.length > 0 && {
      tags: filters.tags.join(","),
    }),
    ...(messageThreadId && { messageThreadId: messageThreadId.toString() }),
    ...(link && { link: encodeURIComponent(link) }),
    ...(filters.dateType && { dateType: filters.dateType }),
    ...(filters.dateRange && { dateRange: filters.dateRange.join(",") }),
    ...(filters.sizeRange && { sizeRange: filters.sizeRange.join(",") }),
    ...(filters.sizeUnit && { sizeUnit: filters.sizeUnit }),
    ...(filters.sort && { sort: filters.sort }),
    ...(filters.order && { order: filters.order }),
  });

  if (page === 0) {
    return `${url}?${params.toString()}`;
  }

  if (!previousPageData) {
    return null;
  }

  params.set("fromMessageId", previousPageData.nextFromMessageId.toString());
  if (filters.offline && previousPageData.files.length > 0) {
    const lastFile = previousPageData.files[previousPageData.files.length - 1]!;
    if (filters.sort === "size") {
      params.set("fromSortField", lastFile.size.toString());
    } else if (filters.sort === "completion_date") {
      params.set("fromSortField", lastFile.completionDate.toString());
    } else if (filters.sort === "date") {
      params.set("fromSortField", lastFile.date.toString());
    } else if (filters.sort === "reaction_count") {
      params.set("fromSortField", lastFile.reactionCount.toString());
    }
  }

  return `${url}?${params.toString()}`;
}
