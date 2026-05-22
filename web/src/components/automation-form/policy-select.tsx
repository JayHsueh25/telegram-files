import React, { useMemo, useState } from "react";
import { Button } from "@/components/ui/button";
import {
  Command,
  CommandGroup,
  CommandItem,
  CommandList,
} from "@/components/ui/command";
import {
  HoverCard,
  HoverCardContent,
  HoverCardTrigger,
} from "@/components/ui/hover-card";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { useMutationObserver } from "@/hooks/use-mutation-observer";
import {
  DuplicationPolicies,
  type DuplicationPolicy,
  TransferPolices,
  type TransferPolicy,
} from "@/lib/types";
import { cn } from "@/lib/utils";
import { Check, ChevronsUpDown } from "lucide-react";

const PolicyLegends: Record<
  TransferPolicy | DuplicationPolicy,
  {
    title: string;
    description: string | React.ReactNode;
  }
> = {
  DIRECT: {
    title: "Direct",
    description: "Transfer files directly to the destination folder.",
  },
  GROUP_BY_CHAT: {
    title: "Group by Chat",
    description: (
      <div className="space-y-2">
        <p className="text-sm">
          Transfer files to folders based on the chat name.
        </p>
        <p className="text-xs text-muted-foreground">Example:</p>
        <p className="inline-block rounded bg-gray-100 p-1 text-xs text-muted-foreground dark:bg-gray-800 dark:text-gray-300">
          {"/${Destination Folder}/${Telegram Id}/${Chat Id}/${file}"}
        </p>
      </div>
    ),
  },
  GROUP_BY_TYPE: {
    title: "Group by Type",
    description: (
      <div className="space-y-2">
        <p className="text-sm">
          Transfer files to folders based on the file type. <br />
          All account files will be transferred to the same folder.
        </p>
        <p className="text-xs text-muted-foreground">Example:</p>
        <p className="inline-block rounded bg-gray-100 p-1 text-xs text-muted-foreground dark:bg-gray-800 dark:text-gray-300">
          {"/${Destination Folder}/${File Type}/${file}"}
        </p>
      </div>
    ),
  },
  GROUP_BY_AI: {
    title: "Group by AI",
    description: (
      <div className="space-y-2">
        <p className="text-sm">
          Use AI to classify files and transfer them to different folders based
          on their content.
        </p>
        <p className="text-sm">
          You can write a prompt to guide the AI in classifying the files. Like:
        </p>
        <p className="inline-block rounded bg-gray-100 p-1 text-xs text-muted-foreground dark:bg-gray-800 dark:text-gray-300">
          Classify the following file into one of the categories: Work,
          Personal, Important, Others. <br />
          File name: {"{file_name}"} <br />
          Respond with only the category name.
        </p>
        <p className="text-sm">
          You can use {"{FileRecord Field}"} in the prompt to provide more
          context to the AI.
        </p>
      </div>
    ),
  },
  OVERWRITE: {
    title: "Overwrite",
    description:
      "If destination exists same name file, move and overwrite the file.",
  },
  SKIP: {
    title: "Skip",
    description:
      "If destination exists same name file, skip the file, nothing to do.",
  },
  RENAME: {
    title: "Rename",
    description:
      "This strategy will rename the file, add a serial number after the file name, and then move the file to the destination folder",
  },
  HASH: {
    title: "Hash",
    description:
      "Calculate the hash (md5) of the file and compare with the existing file, if the hash is the same, delete the original file and set the local path to the existing file, otherwise, move the file",
  },
};

interface PolicySelectProps {
  policyType: "transfer" | "duplication";
  value?: string;
  onChange: (value: string) => void;
}

export function PolicySelect({ policyType, value, onChange }: PolicySelectProps) {
  const [open, setOpen] = useState(false);
  const polices =
    policyType === "transfer" ? TransferPolices : DuplicationPolicies;
  const [peekedPolicy, setPeekedPolicy] = useState<string>(value ?? polices[0]);

  const peekPolicyLegend = useMemo(() => {
    return PolicyLegends[peekedPolicy as TransferPolicy | DuplicationPolicy];
  }, [peekedPolicy]);

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          variant="outline"
          role="combobox"
          aria-expanded={open}
          aria-label="Select a policy"
          className="w-full justify-between"
        >
          {value ?? "Select a policy..."}
          <ChevronsUpDown className="opacity-50" />
        </Button>
      </PopoverTrigger>
      <PopoverContent align="start" className="w-[250px] p-0" modal={true}>
        <HoverCard>
          <HoverCardContent
            side="top"
            align="start"
            forceMount
            className="min-h-[150px] w-auto min-w-64 max-w-[380px]"
          >
            <div className="grid gap-2">
              <h4 className="font-medium leading-none">
                {peekPolicyLegend?.title}
              </h4>
              {typeof peekPolicyLegend?.description === "string" ? (
                <p className="text-sm text-muted-foreground">
                  {peekPolicyLegend?.description ?? ""}
                </p>
              ) : (
                peekPolicyLegend?.description
              )}
            </div>
          </HoverCardContent>
          <Command>
            <CommandList className="h-[var(--cmdk-list-height)] max-h-[400px]">
              <HoverCardTrigger />
              <CommandGroup>
                {polices.map((policy) => (
                  <PolicyItem
                    key={policy}
                    policy={policy ?? ""}
                    isSelected={value === policy}
                    onPeek={setPeekedPolicy}
                    onSelect={() => {
                      onChange(policy);
                      setOpen(false);
                    }}
                  />
                ))}
              </CommandGroup>
            </CommandList>
          </Command>
        </HoverCard>
      </PopoverContent>
    </Popover>
  );
}

interface PolicyItemProps {
  policy: string;
  isSelected: boolean;
  onSelect: () => void;
  onPeek: (policy: string) => void;
}

function PolicyItem({ policy, isSelected, onSelect, onPeek }: PolicyItemProps) {
  const ref = React.useRef<HTMLDivElement>(null);

  useMutationObserver(ref, (mutations) => {
    mutations.forEach((mutation) => {
      if (
        mutation.type === "attributes" &&
        mutation.attributeName === "aria-selected" &&
        ref.current?.getAttribute("aria-selected") === "true"
      ) {
        onPeek(policy);
      }
    });
  });

  return (
    <CommandItem
      key={policy}
      onSelect={onSelect}
      ref={ref}
      className="data-[selected=true]:bg-primary data-[selected=true]:text-primary-foreground"
    >
      {policy}
      <Check
        className={cn("ml-auto", isSelected ? "opacity-100" : "opacity-0")}
      />
    </CommandItem>
  );
}
