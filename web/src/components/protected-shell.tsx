"use client";

import { useEffect, useTransition } from "react";
import { usePathname, useRouter } from "next/navigation";
import { useAuth } from "@/hooks/use-auth";
import { SettingsProvider } from "@/hooks/use-settings";
import { TelegramAccountProvider } from "@/hooks/use-telegram-account";
import { WebSocketProvider } from "@/hooks/use-websocket";

export function ProtectedShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const [, startTransition] = useTransition();
  const { authenticated, isLoading } = useAuth();

  useEffect(() => {
    if (isLoading) {
      return;
    }

    if (!authenticated && pathname !== "/login") {
      startTransition(() => {
        router.replace("/login");
      });
      return;
    }

    if (authenticated && pathname === "/login") {
      startTransition(() => {
        router.replace("/");
      });
    }
  }, [authenticated, isLoading, pathname, router, startTransition]);

  if (pathname === "/login") {
    return <>{children}</>;
  }

  if (isLoading || !authenticated) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-background px-4">
        <div className="space-y-2 text-center">
          <p className="text-sm font-medium text-foreground">
            Checking session...
          </p>
          <p className="text-sm text-muted-foreground">
            Waiting for administrator authentication.
          </p>
        </div>
      </main>
    );
  }

  return (
    <WebSocketProvider>
      <SettingsProvider>
        <TelegramAccountProvider>{children}</TelegramAccountProvider>
      </SettingsProvider>
    </WebSocketProvider>
  );
}
