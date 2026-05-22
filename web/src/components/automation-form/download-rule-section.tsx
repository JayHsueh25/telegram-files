import React from "react";
import Link from "next/link";
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "@/components/ui/accordion";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import { Textarea } from "@/components/ui/textarea";
import type { AutoDownloadRule, FileType } from "@/lib/types";
import { X } from "lucide-react";

interface DownloadRuleSectionProps {
  value: AutoDownloadRule;
  onChange: (value: AutoDownloadRule) => void;
}

export function DownloadRuleSection({
  value,
  onChange,
}: DownloadRuleSectionProps) {
  const handleQueryChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    onChange({
      ...value,
      query: e.target.value,
    });
  };

  const handleFilterExprChange = (
    e: React.ChangeEvent<HTMLTextAreaElement>,
  ) => {
    onChange({
      ...value,
      filterExpr: e.target.value,
    });
  };

  const handleFileTypeSelect = (type: string) => {
    if (value.fileTypes.includes(type as Exclude<FileType, "media">)) {
      return;
    }

    onChange({
      ...value,
      fileTypes: [...value.fileTypes, type as Exclude<FileType, "media">],
    });
  };

  const removeFileType = (typeToRemove: string) => {
    onChange({
      ...value,
      fileTypes: value.fileTypes.filter((type) => type !== typeToRemove),
    });
  };

  return (
    <Accordion type="single" collapsible>
      <AccordionItem value="advanced">
        <AccordionTrigger className="hover:no-underline">
          Advanced
        </AccordionTrigger>
        <AccordionContent>
          <div className="flex flex-col space-y-4 rounded-md border p-4 shadow">
            <div className="flex flex-col space-y-2">
              <Label htmlFor="query-keyword">Query Keyword</Label>
              <Input
                id="query-keyword"
                type="text"
                className="w-full"
                placeholder="Enter a keyword to filter files"
                value={value.query}
                onChange={handleQueryChange}
              />
            </div>
            <div className="flex flex-col space-y-2">
              <Label htmlFor="filter-expr">
                Filter Expression
                <Link
                  href="https://github.com/jarvis2f/telegram-files/blob/main/misc/filter-expression-guide.md"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="ml-2 text-sm text-blue-600 hover:underline"
                >
                  (Learn more)
                </Link>
              </Label>
              <Textarea
                id="filter-expr"
                className="w-full"
                placeholder="Enter a filter expression (e.g., str:contains(content.text.text, 'Hello') and id > 1000)"
                value={value.filterExpr}
                onChange={handleFilterExprChange}
              />
            </div>

            <div className="flex flex-col space-y-2">
              <Label htmlFor="fileTypes">Filter File Types</Label>
              <Select onValueChange={handleFileTypeSelect}>
                <SelectTrigger id="fileTypes">
                  <SelectValue placeholder="Select File Types" />
                </SelectTrigger>
                <SelectContent>
                  {["photo", "video", "audio", "file"].map((type) => (
                    <SelectItem key={type} value={type}>
                      {type}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>

              <div className="mt-2 flex flex-wrap gap-2">
                {value.fileTypes.map((type) => (
                  <Badge
                    key={type}
                    className="flex items-center gap-1 px-2 py-1"
                    variant="secondary"
                  >
                    {type}
                    <X
                      className="h-3 w-3 cursor-pointer"
                      onClick={() => removeFileType(type)}
                    />
                  </Badge>
                ))}
              </div>
            </div>

            <div className="rounded-md border p-4">
              <div className="flex items-center justify-between">
                <Label htmlFor="download-history">Download History</Label>
                <Switch
                  id="download-history"
                  checked={value.downloadHistory}
                  onCheckedChange={(checked) =>
                    onChange({
                      ...value,
                      downloadHistory: checked,
                    })
                  }
                />
              </div>
              <p className="mt-2 text-xs text-muted-foreground">
                If enabled, all historical files will be downloaded. Otherwise,
                only new files will be downloaded.
              </p>
            </div>
            <div className="rounded-md border p-4">
              <div className="flex items-center justify-between">
                <Label htmlFor="download-comment-files">
                  Download comment files
                </Label>
                <Switch
                  id="download-comment-files"
                  checked={value.downloadCommentFiles}
                  onCheckedChange={(checked) =>
                    onChange({
                      ...value,
                      downloadCommentFiles: checked,
                    })
                  }
                />
              </div>
            </div>
          </div>
        </AccordionContent>
      </AccordionItem>
    </Accordion>
  );
}
