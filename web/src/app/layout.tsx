import "@/styles/globals.css";
import type { Metadata } from "next";
import { Inter } from "next/font/google";
import React from "react";
import { Toaster } from "@/components/ui/toaster";
import { SWRProvider } from "@/components/swr-provider";
import { env } from "@/env";
import { ThemeProvider } from "@/components/theme-provider";
import { LocalStorageProvider } from "@/hooks/use-local-storage";
import { AuthProvider } from "@/hooks/use-auth";
import { ProtectedShell } from "@/components/protected-shell";

const inter = Inter({ subsets: ["latin"] });

export const metadata: Metadata = {
  title: "Telegram Files",
  description: "Manage your files on Telegram",
};

export default async function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" suppressHydrationWarning>
      <head>
        <link
          rel="icon"
          type="image/png"
          href="/favicon-96x96.png"
          sizes="96x96"
        />
        <link rel="icon" type="image/svg+xml" href="/favicon.svg" />
        <link rel="shortcut icon" href="/favicon.ico" />
        <link
          rel="apple-touch-icon"
          sizes="180x180"
          href="/apple-touch-icon.png"
        />
        <meta name="apple-mobile-web-app-title" content="TeleFiles" />
        <link rel="manifest" href="/site.webmanifest" />
        {env.NEXT_PUBLIC_SCAN && (
          <script
            src="https://unpkg.com/react-scan/dist/auto.global.js"
            async
          />
        )}
      </head>
      <body className={inter.className}>
        <LocalStorageProvider>
          <ThemeProvider
            attribute="class"
            defaultTheme="light"
            disableTransitionOnChange
          >
            <SWRProvider>
              <AuthProvider>
                <ProtectedShell>{children}</ProtectedShell>
              </AuthProvider>
            </SWRProvider>
            <Toaster />
          </ThemeProvider>
        </LocalStorageProvider>
      </body>
    </html>
  );
}
