import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import type { Auto } from "@/lib/types";
import { DownloadRuleSection } from "./automation-form/download-rule-section";
import { TransferRuleSection } from "./automation-form/transfer-rule-section";

interface AutomationFormProps {
  auto: Auto;
  onChange: (auto: Auto) => void;
}

export default function AutomationForm({
  auto,
  onChange,
}: AutomationFormProps) {
  return (
    <div className="space-y-4">
      <div className="space-y-4 rounded-md border border-gray-200 p-4 dark:border-gray-700">
        <div className="flex items-center justify-between">
          <Label htmlFor="enable-preload">Enable Preload</Label>
          <Switch
            id="enable-preload"
            checked={auto.preload.enabled}
            onCheckedChange={(checked) => {
              onChange({
                ...auto,
                preload: {
                  ...auto.preload,
                  enabled: checked,
                },
              });
            }}
          />
        </div>
        {auto.preload.enabled && (
          <div className="space-y-4 rounded-md border border-gray-200 bg-gray-50 p-4 dark:border-gray-700 dark:bg-gray-800">
            <div className="flex items-start">
              <span className="mr-3 mt-1.5 h-3 w-2 flex-shrink-0 rounded-full bg-cyan-400"></span>
              <p className="text-sm leading-6 text-gray-700 dark:text-gray-300">
                This will enable preload for this chat. All files will be
                loaded, but not downloaded, then you can search offline.
              </p>
            </div>
          </div>
        )}
      </div>
      <div className="space-y-4 rounded-md border border-gray-200 p-4 dark:border-gray-700">
        <div className="flex items-center justify-between">
          <Label htmlFor="enable-auto-download">Enable Auto Download</Label>
          <Switch
            id="enable-auto-download"
            checked={auto.download.enabled}
            onCheckedChange={(checked) => {
              onChange({
                ...auto,
                download: {
                  ...auto.download,
                  enabled: checked,
                },
              });
            }}
          />
        </div>
        {auto.download.enabled && (
          <>
            <div className="space-y-4 rounded-md border border-gray-200 bg-gray-50 p-4 dark:border-gray-700 dark:bg-gray-800">
              <div className="flex items-start">
                <span className="mr-3 mt-1.5 h-3 w-2 flex-shrink-0 rounded-full bg-cyan-400"></span>
                <p className="text-sm leading-6 text-gray-700 dark:text-gray-300">
                  This will enable auto download for this chat. Files will be
                  downloaded automatically.
                </p>
              </div>
              <div className="flex items-start">
                <span className="mr-3 mt-1.5 h-3 w-2 flex-shrink-0 rounded-full bg-cyan-400"></span>
                <p className="text-sm leading-6 text-gray-700 dark:text-gray-300">
                  If you enable download history, the files in historical
                  messages will be downloaded first, and then files in new
                  messages will be downloaded automatically.
                </p>
              </div>
              <div className="flex items-start">
                <span className="mr-3 mt-1.5 h-3 w-2 flex-shrink-0 rounded-full bg-cyan-400"></span>
                <p className="text-sm leading-6 text-gray-700 dark:text-gray-300">
                  Download Order:
                  <span className="ml-1 rounded bg-blue-100 px-2 text-blue-700 dark:bg-blue-800 dark:text-blue-200">
                    {"Photo -> Video -> Audio -> File"}
                  </span>
                </p>
              </div>
            </div>
            <DownloadRuleSection
              value={auto.download.rule}
              onChange={(value) => {
                onChange({
                  ...auto,
                  download: {
                    ...auto.download,
                    rule: value,
                  },
                });
              }}
            />
          </>
        )}
      </div>
      <div className="space-y-4 rounded-md border border-gray-200 p-4 dark:border-gray-700">
        <div className="flex items-center justify-between">
          <Label htmlFor="enable-transfer">Enable Transfer</Label>
          <Switch
            id="enable-transfer"
            checked={auto.transfer.enabled}
            onCheckedChange={(checked) => {
              onChange({
                ...auto,
                transfer: {
                  ...auto.transfer,
                  enabled: checked,
                },
              });
            }}
          />
        </div>
        {auto.transfer.enabled && (
          <>
            <div className="space-y-4 rounded-md border border-gray-200 bg-gray-50 p-4 dark:border-gray-700 dark:bg-gray-800">
              <div className="flex items-start">
                <span className="mr-3 mt-1.5 h-3 w-2 flex-shrink-0 rounded-full bg-cyan-400"></span>
                <p className="text-sm leading-6 text-gray-700 dark:text-gray-300">
                  This will enable auto transfer for this chat. Downloaded files
                  will be transferred to the specified location automatically.
                </p>
              </div>
            </div>
            <TransferRuleSection
              value={auto.transfer.rule}
              onChange={(value) => {
                onChange({
                  ...auto,
                  transfer: {
                    ...auto.transfer,
                    rule: value,
                  },
                });
              }}
            />
          </>
        )}
      </div>
    </div>
  );
}
