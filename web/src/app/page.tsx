"use client";

import { Header } from "@/components/header";
import { MobileHeader } from "@/components/mobile/mobile-header";
import { EmptyState } from "@/components/empty-state";
import useIsMobile from "@/hooks/use-is-mobile";
import { useTelegramAccount } from "@/hooks/use-telegram-account";
import { TelegramChatProvider } from "@/hooks/use-telegram-chat";

export default function Home() {
  const { getAccounts, handleAccountChange, isLoading } = useTelegramAccount();
  const isMobile = useIsMobile();
  const accounts = getAccounts();

  return (
    <TelegramChatProvider>
      <div className="container mx-auto px-4 pt-6">
        {isMobile ? <MobileHeader /> : <Header />}
      </div>
      <EmptyState
        isLoadingAccount={isLoading}
        hasAccounts={(accounts ?? []).length > 0}
        accounts={accounts}
        onSelectAccount={handleAccountChange}
      />
    </TelegramChatProvider>
  );
}
