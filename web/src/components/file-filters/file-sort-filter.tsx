import { ArrowDownNarrowWide, ArrowUpNarrowWide } from "lucide-react";
import type { FileFilter, SortFields } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { cn } from "@/lib/utils";

interface SortFilterProps {
  sort: SortFields | undefined;
  order: "asc" | "desc" | undefined;
  onChange: (sort: SortFields, order: "asc" | "desc") => void;
}

const SortFilter = ({ sort, order, onChange }: SortFilterProps) => {
  const currentSort = sort ?? "date";
  const currentOrder = order ?? "desc";

  const sortOptions = [
    { value: "date", label: "Sent Date" },
    { value: "completion_date", label: "Downloaded Date" },
    { value: "size", label: "File Size" },
    { value: "reaction_count", label: "Reaction Count" },
  ] as const;

  return (
    <div className="space-y-2">
      <Label>Sort By</Label>
      <div className="flex gap-2">
        <Select
          value={currentSort}
          onValueChange={(newSort: typeof currentSort) =>
            onChange(newSort, currentOrder)
          }
        >
          <SelectTrigger className="flex-1">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {sortOptions.map((option) => (
              <SelectItem key={option.value} value={option.value}>
                {option.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Button
          variant="outline"
          size="icon"
          onClick={() =>
            onChange(currentSort, currentOrder === "asc" ? "desc" : "asc")
          }
          className={cn("h-9 w-9")}
        >
          {currentOrder === "asc" ? (
            <ArrowUpNarrowWide className="h-4 w-4" />
          ) : (
            <ArrowDownNarrowWide className="h-4 w-4" />
          )}
        </Button>
      </div>
    </div>
  );
};

interface FileSortFilterProps {
  value: FileFilter;
  onChange: (value: FileFilter) => void;
}

export function FileSortFilter({ value, onChange }: FileSortFilterProps) {
  const handleSortChange = (sort: SortFields, order: "asc" | "desc") => {
    onChange({ ...value, sort, order });
  };

  return (
    <SortFilter
      sort={value.sort}
      order={value.order}
      onChange={handleSortChange}
    />
  );
}
