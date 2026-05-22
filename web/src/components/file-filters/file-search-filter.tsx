import { useState } from "react";
import { X } from "lucide-react";
import type {
  DownloadStatus,
  FileFilter,
  FileType,
  TransferStatus,
} from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { TagsSelector } from "@/components/ui/tags-selector";
import FileStatusFilter from "@/components/file-status-filter";
import FileTypeFilter from "@/components/file-type-filter";
import { useSettings } from "@/hooks/use-settings";
import { split } from "@/lib/utils";

const SearchFilter = ({
  search,
  onChange,
}: {
  search: string;
  onChange: (search: string) => void;
}) => {
  const [localSearch, setLocalSearch] = useState(search);

  const handleChange = (value: string) => {
    setLocalSearch(value);
    onChange(value);
  };

  return (
    <div className="space-y-2">
      <Label>Keyword</Label>
      <div className="relative">
        <Input
          placeholder="Search with name or caption"
          value={localSearch}
          onChange={(e) => handleChange(e.target.value)}
        />
        {search && (
          <Button
            variant="ghost"
            size="icon"
            className="absolute right-2 top-1/2 h-6 w-6 -translate-y-1/2 rounded-full text-gray-500 transition-all duration-200 hover:scale-110 hover:bg-gray-100 hover:text-gray-800"
            onClick={() => handleChange("")}
          >
            <X className="h-4 w-4" />
          </Button>
        )}
      </div>
    </div>
  );
};

interface TagsFilterProps {
  tags: string[];
  onChange: (tags: string[]) => void;
}

const TagsFilter = ({ tags, onChange }: TagsFilterProps) => {
  const { settings } = useSettings();

  return (
    <div className="space-y-2">
      <Label>Tags</Label>
      <TagsSelector
        value={tags}
        onChangeAction={onChange}
        tags={split(",", settings?.tags)}
      />
    </div>
  );
};

interface FileSearchFilterProps {
  telegramId: string;
  chatId: string;
  value: FileFilter;
  onChange: (value: FileFilter) => void;
}

export function FileSearchFilter({
  telegramId,
  chatId,
  value,
  onChange,
}: FileSearchFilterProps) {
  const handleSearchChange = (search: string) => {
    onChange({ ...value, search });
  };

  const handleTypeChange = (type: FileType | "all") => {
    onChange({ ...value, type });
  };

  const handleStatusChange = (
    downloadStatus?: DownloadStatus,
    transferStatus?: TransferStatus,
  ) => {
    onChange({
      ...value,
      downloadStatus,
      transferStatus,
    });
  };

  const handleTagsChange = (tags: string[]) => {
    onChange({ ...value, tags });
  };

  return (
    <>
      <SearchFilter search={value.search} onChange={handleSearchChange} />

      <FileTypeFilter
        offline={value.offline}
        telegramId={telegramId}
        chatId={chatId}
        type={value.type}
        onChange={handleTypeChange}
      />

      {!value.offline && (
        <div className="flex items-center justify-between rounded-md border bg-gray-100/50 px-2 py-3 dark:bg-gray-600/50">
          <Label htmlFor="notDownload">Filter Not Download</Label>
          <Switch
            id="notDownload"
            checked={value.downloadStatus === "idle"}
            onCheckedChange={(checked) => {
              onChange({
                ...value,
                downloadStatus: checked ? "idle" : undefined,
              });
            }}
            aria-label="Not Download"
          />
        </div>
      )}

      {value.offline && (
        <>
          <FileStatusFilter
            downloadStatus={value.downloadStatus}
            transferStatus={value.transferStatus}
            onChange={handleStatusChange}
          />

          <TagsFilter tags={value.tags} onChange={handleTagsChange} />
        </>
      )}
    </>
  );
}
