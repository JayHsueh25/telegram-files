"use client";

import React, { useState } from "react";
import { KeyRound } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { VisuallyHidden } from "@radix-ui/react-visually-hidden";
import { SecuritySettingsForm } from "@/components/security-settings-form";

type AccountSecurityDialogProps = {
  compact?: boolean;
};

export function AccountSecurityDialog({
  compact = false,
}: AccountSecurityDialogProps) {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <Dialog open={isOpen} onOpenChange={setIsOpen}>
      <DialogTrigger asChild>
        {compact ? (
          <Button variant="ghost" size="icon" aria-label="账号安全">
            <KeyRound className="h-4 w-4" />
          </Button>
        ) : (
          <Button variant="outline" size="sm" className="gap-2">
            <KeyRound className="h-4 w-4" />
            账号安全
          </Button>
        )}
      </DialogTrigger>
      <DialogContent
        className="w-full max-w-xl"
        aria-describedby={undefined}
        onPointerDownOutside={() => setIsOpen(false)}
      >
        <VisuallyHidden>
          <DialogTitle>账号安全</DialogTitle>
        </VisuallyHidden>
        <SecuritySettingsForm />
      </DialogContent>
    </Dialog>
  );
}
