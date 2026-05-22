import * as React from "react";
import { type CSSProperties, useEffect, useState } from "react";
import { Filter } from "lucide-react";
import { type FileFilter } from "@/lib/types";
import { Button } from "./ui/button";
import {
  Drawer,
  DrawerDescription,
  DrawerFooter,
  DrawerOverlay,
  DrawerPortal,
  DrawerTitle,
  DrawerTrigger,
} from "./ui/drawer";
import { Drawer as DrawerPrimitive } from "vaul";
import { Label } from "@/components/ui/label";
import { cn } from "@/lib/utils";
import { Switch } from "@/components/ui/switch";
import useIsMobile from "@/hooks/use-is-mobile";
import { FileDateSizeFilter } from "@/components/file-filters/file-date-size-filter";
import { FileSearchFilter } from "@/components/file-filters/file-search-filter";
import { FileSortFilter } from "@/components/file-filters/file-sort-filter";

interface FileFiltersProps {
  telegramId: string;
  chatId: string;
  filters: FileFilter;
  onFiltersChange: (filters: FileFilter) => void;
  clearFilters: () => void;
}

export default function FileFilters({
  telegramId,
  chatId,
  filters,
  onFiltersChange,
  clearFilters,
}: FileFiltersProps) {
  const noAccountSpecified = telegramId === "-1" && chatId === "-1";
  const [localFilters, setLocalFilters] = useState<FileFilter>(filters);
  const isMobile = useIsMobile();
  const [open, setOpen] = useState(false);

  useEffect(() => {
    setLocalFilters(filters);
  }, [filters]);

  const filterCount = Object.entries(filters).filter(([key, value]) => {
    if (["offline", "sort", "order", "dateType", "sizeUnit"].includes(key))
      return false;
    if (typeof value === "string") return value !== "";
    if (typeof value === "boolean") return value;
    if (Array.isArray(value)) return value.length > 0;
    return false;
  }).length;

  const handleApply = () => {
    onFiltersChange(localFilters);
    setOpen(false);
  };

  const handleClear = () => {
    clearFilters();
    setOpen(false);
  };

  return (
    <Drawer
      open={open}
      onOpenChange={setOpen}
      direction={isMobile ? "bottom" : "left"}
      shouldScaleBackground={isMobile}
      preventScrollRestoration={true}
    >
      <DrawerTrigger asChild>
        <Button
          variant="outline"
          className={cn("relative gap-2", isMobile && "z-50 w-9")}
        >
          <Filter className="h-5 w-5" />
          {!isMobile && "Filters"}
          {filterCount > 0 && (
            <span className="absolute left-0 top-0 -ml-1 -mt-1 flex h-6 w-6 items-center justify-center rounded-full bg-red-500 text-xs text-white">
              {filterCount}
            </span>
          )}
        </Button>
      </DrawerTrigger>
      <DrawerPortal>
        <DrawerOverlay className="bg-black/30 dark:bg-black/50" />
        <DrawerPrimitive.Content
          className={cn(
            isMobile
              ? "fixed inset-x-0 bottom-0 z-50 mt-24 flex h-auto max-h-screen flex-col rounded-t-[10px] border bg-background"
              : "fixed bottom-2 left-2 top-2 z-50 flex w-[380px] outline-none",
          )}
          style={
            isMobile
              ? {}
              : ({ "--initial-transform": "calc(100% + 8px)" } as CSSProperties)
          }
        >
          {isMobile && (
            <div className="mx-auto mt-4 h-2 w-[100px] rounded-full bg-muted" />
          )}
          <div className="no-scrollbar flex h-full w-full grow flex-col overflow-auto rounded-[16px] bg-background shadow-lg">
            <div className="flex-1 p-6 pb-[130px]">
              <DrawerTitle>
                <div className="flex items-center justify-between">
                  <span className="text-xl font-semibold text-zinc-900 dark:text-zinc-100">
                    Filters
                  </span>
                  {!noAccountSpecified && (
                    <div className="flex items-center space-x-2">
                      <Label
                        htmlFor="offline"
                        className="cursor-pointer text-zinc-500"
                      >
                        Offline
                      </Label>
                      <Switch
                        id="offline"
                        checked={localFilters.offline}
                        onCheckedChange={(checked) => {
                          setLocalFilters((prev) => ({
                            ...prev,
                            offline: checked,
                          }));
                        }}
                      />
                    </div>
                  )}
                </div>
              </DrawerTitle>
              <DrawerDescription className="mb-3">
                Default search by Telegram Client, you can choose offline to
                search by local database.
              </DrawerDescription>

              <div className="space-y-4 overflow-y-auto p-0.5">
                <FileSearchFilter
                  telegramId={telegramId}
                  chatId={chatId}
                  value={localFilters}
                  onChange={setLocalFilters}
                />

                {localFilters.offline && (
                  <>
                    <FileDateSizeFilter
                      value={localFilters}
                      onChange={setLocalFilters}
                    />

                    <FileSortFilter
                      value={localFilters}
                      onChange={setLocalFilters}
                    />
                  </>
                )}
              </div>
            </div>

            <DrawerFooter className="fixed bottom-0 left-0 right-0 bg-background">
              <Button onClick={handleApply}>Apply Filters</Button>
              <Button variant="outline" onClick={handleClear}>
                Clear Filters
              </Button>
            </DrawerFooter>
          </div>
        </DrawerPrimitive.Content>
      </DrawerPortal>
    </Drawer>
  );
}
