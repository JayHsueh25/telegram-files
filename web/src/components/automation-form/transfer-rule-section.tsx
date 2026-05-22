import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "@/components/ui/accordion";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { Textarea } from "@/components/ui/textarea";
import {
  type AutoTransferRule,
  type DuplicationPolicy,
  type TransferPolicy,
} from "@/lib/types";
import { PolicySelect } from "./policy-select";

interface TransferRuleSectionProps {
  value: AutoTransferRule;
  onChange: (value: AutoTransferRule) => void;
}

export function TransferRuleSection({
  value,
  onChange,
}: TransferRuleSectionProps) {
  const handleTransferRuleChange = (changes: Partial<AutoTransferRule>) => {
    onChange({
      ...value,
      ...changes,
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
              <Label htmlFor="destination">
                Destination folder for auto transfer
              </Label>
              <Input
                id="destination"
                type="text"
                className="w-full"
                placeholder="Enter a destination folder"
                value={value.destination}
                onChange={(e) => {
                  handleTransferRuleChange({ destination: e.target.value });
                }}
              />
            </div>

            <div className="flex flex-col space-y-2">
              <Label htmlFor="transfer-policy">Transfer Policy</Label>
              <PolicySelect
                policyType="transfer"
                value={value.transferPolicy}
                onChange={(policy) =>
                  handleTransferRuleChange({
                    transferPolicy: policy as TransferPolicy,
                  })
                }
              />
            </div>

            {value.transferPolicy === "GROUP_BY_AI" && (
              <div className="flex flex-col space-y-2">
                <Label htmlFor="prompt-template">
                  AI Classification Prompt Template
                </Label>
                <Textarea
                  id="prompt-template"
                  className="w-full"
                  rows={4}
                  placeholder="Enter a prompt template to guide AI classification"
                  value={value.extra.promptTemplate || ""}
                  onChange={(e) =>
                    handleTransferRuleChange({
                      extra: {
                        ...value.extra,
                        promptTemplate: e.target.value,
                      },
                    })
                  }
                />
              </div>
            )}

            <div className="flex flex-col space-y-2">
              <Label htmlFor="duplication-policy">Duplication Policy</Label>
              <PolicySelect
                policyType="duplication"
                value={value.duplicationPolicy}
                onChange={(policy) =>
                  handleTransferRuleChange({
                    duplicationPolicy: policy as DuplicationPolicy,
                  })
                }
              />
            </div>

            <div className="rounded-md border p-4">
              <div className="flex items-center justify-between">
                <Label htmlFor="transfer-history">Transfer History</Label>
                <Switch
                  id="transfer-history"
                  checked={value.transferHistory}
                  onCheckedChange={(checked) =>
                    handleTransferRuleChange({ transferHistory: checked })
                  }
                />
              </div>
              <p className="mt-2 text-xs text-muted-foreground">
                Transfer files that are already downloaded to the specified
                location.
              </p>
            </div>
          </div>
        </AccordionContent>
      </AccordionItem>
    </Accordion>
  );
}
