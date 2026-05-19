"use client";

import React, {
  createContext,
  useContext,
  useEffect,
  useState,
  useTransition,
} from "react";
import { usePathname, useRouter } from "next/navigation";
import useSWR from "swr";
import { POST, registerUnauthorizedHandler, request } from "@/lib/api";
import { type AdminCredentialsInput, type AuthSession } from "@/lib/types";

type AuthContextType = {
  isLoading: boolean;
  session?: AuthSession;
  authenticated: boolean;
  username?: string | null;
  login: (input: AdminCredentialsInput) => Promise<void>;
  logout: () => Promise<void>;
  refreshSession: () => Promise<AuthSession | undefined>;
};

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const [, startTransition] = useTransition();
  const [hasLoadedOnce, setHasLoadedOnce] = useState(false);
  const { data, isLoading, mutate } = useSWR<AuthSession>(
    "/auth/session",
    request,
    {
      revalidateOnFocus: false,
    },
  );

  useEffect(() => {
    if (!isLoading) {
      setHasLoadedOnce(true);
    }
  }, [isLoading]);

  useEffect(() => {
    registerUnauthorizedHandler(() => {
      void mutate({ authenticated: false, username: null }, false);
      if (pathname !== "/login") {
        startTransition(() => {
          router.replace("/login");
        });
      }
    });
    return () => registerUnauthorizedHandler(null);
  }, [mutate, pathname, router, startTransition]);

  const authenticated = Boolean(data?.authenticated);
  const username = data?.username ?? null;

  return (
    <AuthContext.Provider
      value={{
        isLoading: !hasLoadedOnce && isLoading,
        session: data,
        authenticated,
        username,
        login: async (input) => {
          await POST("/auth/login", input);
          await mutate();
        },
        logout: async () => {
          await POST("/auth/logout");
          await mutate({ authenticated: false, username: null }, false);
          startTransition(() => {
            router.replace("/login");
          });
        },
        refreshSession: async () => await mutate(),
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
