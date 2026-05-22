import { useState } from "react";
import { format } from "date-fns";
import { Calendar as CalendarRange } from "lucide-react";
import type { FileFilter } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Calendar } from "@/components/ui/calendar";
import { Label } from "@/components/ui/label";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { RangeSlider } from "@/components/ui/slider";
import useIsMobile from "@/hooks/use-is-mobile";

interface DateFilterProps {
  dateType: "sent" | "downloaded" | undefined;
  dateRange: [string, string] | undefined;
  onChange: (type: "sent" | "downloaded", range: [string, string]) => void;
}

const DateFilter = ({ dateType, dateRange, onChange }: DateFilterProps) => {
  const [open, setOpen] = useState(false);
  const isMobile = useIsMobile();
  const [localType, setLocalType] = useState<"sent" | "downloaded">(
    dateType ?? "sent",
  );
  const [localRange, setLocalRange] = useState<
    [Date | undefined, Date | undefined]
  >([
    dateRange?.[0] ? new Date(dateRange[0]) : undefined,
    dateRange?.[1] ? new Date(dateRange[1]) : undefined,
  ]);

  const handleTypeChange = (type: "sent" | "downloaded") => {
    setLocalType(type);
  };

  const handleRangeSelect = (range?: {
    from: Date | undefined;
    to?: Date | undefined;
  }) => {
    if (!range) return;

    setLocalRange([range.from, range.to]);
    if (range.from && range.to) {
      onChange(localType, [
        format(range.from, "yyyy-MM-dd"),
        format(range.to, "yyyy-MM-dd"),
      ]);
    }
  };

  const getDisplayText = () => {
    if (!dateRange?.[0] && !dateRange?.[1]) return "Select date range";
    if (dateRange[0] && dateRange[1]) {
      return `${format(new Date(dateRange[0]), "LLL dd, y")} - ${format(new Date(dateRange[1]), "LLL dd, y")}`;
    }
    return "Date range selected";
  };

  return (
    <div className="space-y-2">
      <Label>Date Filter</Label>
      <Popover open={open} onOpenChange={setOpen}>
        <PopoverTrigger asChild>
          <Button
            variant="outline"
            className="w-full justify-start text-left font-normal"
          >
            <CalendarRange className="mr-2 h-4 w-4" />
            <span className="flex-1">{getDisplayText()}</span>
            <span className="ml-2 rounded bg-zinc-100 px-2 py-0.5 text-xs text-zinc-600">
              {localType === "downloaded" ? "Download" : "Sent"}
            </span>
          </Button>
        </PopoverTrigger>
        <PopoverContent
          className="w-auto p-4"
          side={isMobile ? undefined : "right"}
          modal={true}
        >
          <div className="space-y-4">
            <div className="flex gap-2">
              <Button
                size="sm"
                variant={localType === "sent" ? "default" : "outline"}
                onClick={() => handleTypeChange("sent")}
                className="flex-1"
              >
                Sent Date
              </Button>
              <Button
                size="sm"
                variant={localType === "downloaded" ? "default" : "outline"}
                onClick={() => handleTypeChange("downloaded")}
                className="flex-1"
              >
                Downloaded
              </Button>
            </div>
            <div className="rounded-md border p-2">
              <Calendar
                mode="range"
                selected={{
                  from: localRange[0],
                  to: localRange[1],
                }}
                onSelect={handleRangeSelect}
                numberOfMonths={2}
                defaultMonth={localRange[0] ?? new Date()}
              />
            </div>
          </div>
        </PopoverContent>
      </Popover>
    </div>
  );
};

interface SizeFilterProps {
  sizeRange: [number, number] | undefined;
  sizeUnit: "KB" | "MB" | "GB" | undefined;
  onChange: (range: [number, number], unit: "KB" | "MB" | "GB") => void;
}

const SizeFilter = ({ sizeRange, sizeUnit, onChange }: SizeFilterProps) => {
  const defaultRange: [number, number] = [0, 1000];
  const [localRange, setLocalRange] = useState<[number, number]>(
    sizeRange ?? defaultRange,
  );
  const [localUnit, setLocalUnit] = useState<"KB" | "MB" | "GB">(
    sizeUnit ?? "MB",
  );

  const handleChange = (newValue: number[]) => {
    const range: [number, number] = [newValue[0]!, newValue[1]!];
    setLocalRange(range);
    onChange(range, localUnit);
  };

  const handleUnitChange = (unit: "KB" | "MB" | "GB") => {
    setLocalUnit(unit);
    onChange(localRange, unit);
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <Label>File Size Range</Label>
        <Popover>
          <PopoverTrigger asChild>
            <Button variant="outline" size="sm">
              {localUnit}
            </Button>
          </PopoverTrigger>
          <PopoverContent className="w-auto p-2" align="center" modal={true}>
            <div className="flex flex-col gap-2">
              <Button
                size="sm"
                variant={localUnit === "KB" ? "default" : "outline"}
                onClick={() => handleUnitChange("KB")}
              >
                KB
              </Button>
              <Button
                size="sm"
                variant={localUnit === "MB" ? "default" : "outline"}
                onClick={() => handleUnitChange("MB")}
              >
                MB
              </Button>
              <Button
                size="sm"
                variant={localUnit === "GB" ? "default" : "outline"}
                onClick={() => handleUnitChange("GB")}
              >
                GB
              </Button>
            </div>
          </PopoverContent>
        </Popover>
      </div>
      <div
        className="px-2 pt-2"
        onPointerDown={(e) => {
          e.stopPropagation();
        }}
      >
        <RangeSlider
          value={localRange}
          min={0}
          max={1000}
          step={1}
          minStepsBetweenThumbs={1}
          className="w-full"
          onValueChange={handleChange}
        />
      </div>
      <div className="flex justify-between text-sm">
        <span className="text-zinc-500">
          {localRange[0]} {localUnit}
        </span>
        <span className="text-zinc-500">
          {localRange[1]} {localUnit}
        </span>
      </div>
    </div>
  );
};

interface FileDateSizeFilterProps {
  value: FileFilter;
  onChange: (value: FileFilter) => void;
}

export function FileDateSizeFilter({
  value,
  onChange,
}: FileDateSizeFilterProps) {
  const handleDateChange = (
    dateType: "sent" | "downloaded",
    dateRange: [string, string],
  ) => {
    onChange({ ...value, dateType, dateRange });
  };

  const handleSizeChange = (
    sizeRange: [number, number],
    sizeUnit: "KB" | "MB" | "GB",
  ) => {
    onChange({ ...value, sizeRange, sizeUnit });
  };

  return (
    <>
      <DateFilter
        dateType={value.dateType}
        dateRange={value.dateRange}
        onChange={handleDateChange}
      />

      <SizeFilter
        sizeRange={value.sizeRange}
        sizeUnit={value.sizeUnit}
        onChange={handleSizeChange}
      />
    </>
  );
}
