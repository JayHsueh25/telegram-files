import { env } from "@/env";

let unauthorizedHandler: (() => void) | null = null;

export class ApiRequestError extends Error {
  status: number;
  code?: string;

  constructor(status: number, message: string, code?: string) {
    super(message);
    this.status = status;
    this.code = code;
  }
}

export function getApiUrl(): string {
  const url = env.NEXT_PUBLIC_API_URL;
  if (url.startsWith("http")) {
    return url;
  }
  if (typeof window === "undefined") {
    return url;
  }
  return `${window.location.protocol}//${window.location.host}${url}`;
}

export function getWsUrl(): string {
  const url = env.NEXT_PUBLIC_WS_URL;
  if (url.startsWith("ws")) {
    return url;
  }
  if (typeof window === "undefined") {
    return url;
  }
  return `${window.location.protocol === "https:" ? "wss" : "ws"}://${
    window.location.host
  }${url}`;
}

export function registerUnauthorizedHandler(handler: (() => void) | null) {
  unauthorizedHandler = handler;
}

export class UnauthorizedError extends Error {
  constructor(message = "Unauthorized") {
    super(message);
    this.name = "UnauthorizedError";
  }
}

/* eslint-disable */
export async function request<T = any>(
  api: string,
  requestInit?: RequestInit,
): Promise<T> {
  const defaultHeaders = {
    "Content-Type": "application/json",
  };

  const response = await fetch(`${getApiUrl()}${api}`, {
    credentials: "include",
    headers: {
      ...defaultHeaders,
      ...requestInit?.headers,
    },
    ...requestInit,
  });
  const responseText = await response.text();
  if (!responseText) {
    if (!response.ok) {
      throw new ApiRequestError(
        response.status,
        `Request failed with status ${response.status}`,
      );
    }
    return undefined as T;
  }
  let data;
  try {
    data = JSON.parse(responseText);
  } catch (e) {
    if (!response.ok) {
      throw new ApiRequestError(
        response.status,
        `Request failed with status ${response.status}`,
      );
    }
    throw new RequestParsedError(responseText);
  }
  if (response.status === 401) {
    unauthorizedHandler?.();
    throw new UnauthorizedError(data?.error ?? "Unauthorized");
  }
  if (!response.ok) {
    throw new ApiRequestError(
      response.status,
      data.error ?? `Request failed with status ${response.status}`,
      data.code,
    );
  }

  return data as T;
}

export class RequestParsedError extends Error {
  responseText: string;

  constructor(responseText: string) {
    super("Parse JSON Error");
    this.responseText = responseText;
  }
}

export function localStorageProvider() {
  const map = new Map<string, any>(
    JSON.parse(localStorage.getItem("telegram-files") ?? "[]"),
  );

  window.addEventListener("beforeunload", () => {
    const appCache = JSON.stringify(Array.from(map.entries()));
    localStorage.setItem("telegram-files", appCache);
  });

  return map;
}

export async function POST(api: string, data?: any): Promise<any> {
  return await request(api, {
    method: "POST",
    body: JSON.stringify(data),
  });
}

export type TelegramApiArg = {
  data: any;
  method: string;
};

export async function telegramApi(
  api: string,
  {
    arg,
  }: {
    arg: TelegramApiArg;
  },
): Promise<any> {
  return await request(`${api}/${arg.method}`, {
    method: "POST",
    body: arg.data ? JSON.stringify(arg.data) : undefined,
  });
}
