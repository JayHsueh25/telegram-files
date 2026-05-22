# Admin Auth Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 `telegram-files` 增加单管理员账号密码登录、未登录拦截、可修改账号密码的前后端闭环，并优先落实密码哈希、会话鉴权、统一 `401` 处理和最小信息泄漏。

**Architecture:** 以后端 Vert.x Session 作为唯一真实认证边界，管理员账号与密码哈希存入现有 `setting_record`。前端新增认证上下文、登录页和受保护壳层，所有页面和请求都以 `/auth/session` 为准，同步拦住 HTTP 和 WebSocket。

**Tech Stack:** Vert.x 5、Java 23、JDK `PBKDF2WithHmacSHA256`、Next.js 16 App Router、React 19、SWR、JUnit 5

---

## File Map

- Create: `api/src/main/java/telegram/files/AdminAuthService.java`
- Create: `api/src/test/java/telegram/files/AdminAuthServiceTest.java`
- Create: `api/src/test/java/telegram/files/HttpVerticleAuthTest.java`
- Modify: `api/src/main/java/telegram/files/Config.java`
- Modify: `api/src/main/java/telegram/files/HttpVerticle.java`
- Modify: `api/src/main/java/telegram/files/repository/SettingKey.java`
- Create: `web/src/hooks/use-auth.tsx`
- Create: `web/src/components/protected-shell.tsx`
- Create: `web/src/components/login-form.tsx`
- Create: `web/src/components/security-settings-form.tsx`
- Create: `web/src/app/login/page.tsx`
- Modify: `web/src/app/layout.tsx`
- Modify: `web/src/components/header.tsx`
- Modify: `web/src/components/settings-dialog.tsx`
- Modify: `web/src/components/swr-provider.tsx`
- Modify: `web/src/lib/api.ts`
- Modify: `web/src/lib/types.ts`
- Modify: `.env.example`
- Modify: `README.md`

## Security Priorities

1. 密码只存哈希，不存明文；校验使用常量时间比较。
2. 未登录时同时拒绝页面数据请求和 `/ws` 连接，不能只挡前端页面。
3. 登录失败统一返回“账号或密码错误”，不泄漏具体失败原因。
4. 修改账号密码要求当前会话已登录，且只接受合法新值。
5. 前端遇到 `401` 必须立即清理认证状态并跳回登录页，避免半登录状态。

### Task 1: 建立安全的管理员凭据基础

**Files:**
- Create: `api/src/main/java/telegram/files/AdminAuthService.java`
- Create: `api/src/test/java/telegram/files/AdminAuthServiceTest.java`
- Modify: `api/src/main/java/telegram/files/repository/SettingKey.java`
- Modify: `api/src/main/java/telegram/files/Config.java`

- [ ] **Step 1: 先写管理员凭据初始化与哈希校验失败测试**

```java
package telegram.files;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import telegram.files.repository.SettingKey;

@ExtendWith(VertxExtension.class)
public class AdminAuthServiceTest {

    @BeforeEach
    void deployDataVerticle(Vertx vertx, VertxTestContext testContext) {
        vertx.deployVerticle(new DataVerticle())
                .onComplete(testContext.succeedingThenComplete());
    }

    @AfterEach
    void cleanup(Vertx vertx, VertxTestContext testContext) {
        DataVerticleTest.clear(vertx).onComplete(testContext.succeedingThenComplete());
    }

    @Test
    @DisplayName("首次初始化写入默认管理员凭据并使用 PBKDF2 哈希")
    void ensureDefaultCredentialsCreated(Vertx vertx, VertxTestContext testContext) {
        AdminAuthService authService = new AdminAuthService();

        authService.ensureInitialized()
                .compose(_ -> DataVerticle.settingRepository.<String>getByKey(SettingKey.adminUsername))
                .compose(username -> {
                    testContext.verify(() -> Assertions.assertEquals("admin", username));
                    return DataVerticle.settingRepository.<String>getByKey(SettingKey.adminPasswordHash);
                })
                .compose(hash -> {
                    testContext.verify(() -> {
                        Assertions.assertNotNull(hash);
                        Assertions.assertTrue(hash.startsWith("pbkdf2$"));
                    });
                    return authService.verifyCredentials("admin", "admin");
                })
                .onComplete(testContext.succeeding(matches -> testContext.verify(() -> {
                    Assertions.assertTrue(matches);
                    testContext.completeNow();
                })));
    }

    @Test
    @DisplayName("拒绝过短的新密码")
    void rejectShortPassword(Vertx vertx, VertxTestContext testContext) {
        AdminAuthService authService = new AdminAuthService();

        authService.ensureInitialized()
                .compose(_ -> authService.updateCredentials("admin", "123"))
                .onComplete(testContext.failing(err -> testContext.verify(() -> {
                    Assertions.assertTrue(err.getMessage().contains("Password must be at least 4 characters"));
                    testContext.completeNow();
                })));
    }
}
```

- [ ] **Step 2: 运行测试确认当前失败**

Run: `.\gradlew.bat test --tests telegram.files.AdminAuthServiceTest`

Expected:
- `FAILURE: Build failed with an exception.`
- 报错包含 `cannot find symbol class AdminAuthService`

- [ ] **Step 3: 实现最小安全凭据服务**

`api/src/main/java/telegram/files/repository/SettingKey.java`

```java
public enum SettingKey {
    version(Version::new),
    adminUsername,
    adminPasswordHash,
    uniqueOnly(Convert::toBool, false),
    imageLoadSize,
    alwaysHide(Convert::toBool, false),
    showSensitiveContent(Convert::toBool, false),
    automation(value -> StrUtil.isBlank(value) ? null : new JsonObject(value).mapTo(SettingAutoRecords.class)),
    autoDownloadLimit(Convert::toInt),
    autoDownloadTimeLimited(value -> StrUtil.isBlank(value) ? null : new JsonObject(value).mapTo(SettingTimeLimitedDownload.class)),
    proxys(value -> StrUtil.isBlank(value) ? null : new JsonObject(value).mapTo(SettingProxyRecords.class)),
    avgSpeedInterval(Convert::toInt, 5 * 60),
    speedUnits(Function.identity(), "bits"),
    tags(value -> StrUtil.isBlank(value) ? null : StrUtil.split(value, ","));
```

`api/src/main/java/telegram/files/Config.java`

```java
public static final String ADMIN_USERNAME = StrUtil.blankToDefault(System.getenv("ADMIN_USERNAME"), "admin");

public static final String ADMIN_PASSWORD = StrUtil.blankToDefault(System.getenv("ADMIN_PASSWORD"), "admin");
```

`api/src/main/java/telegram/files/AdminAuthService.java`

```java
package telegram.files;

import cn.hutool.core.util.StrUtil;
import io.vertx.core.Future;
import io.vertx.ext.web.Session;
import telegram.files.repository.SettingKey;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class AdminAuthService {

    public static final String SESSION_AUTHENTICATED = "adminAuthenticated";
    public static final String SESSION_USERNAME = "adminUsername";
    private static final int PASSWORD_MIN_LENGTH = 4;
    private static final int PBKDF2_ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256;
    private final SecureRandom secureRandom = new SecureRandom();

    public Future<Void> ensureInitialized() {
        return DataVerticle.settingRepository.<String>getByKey(SettingKey.adminUsername)
                .compose(username -> DataVerticle.settingRepository.<String>getByKey(SettingKey.adminPasswordHash)
                        .compose(hash -> {
                            if (StrUtil.isNotBlank(username) && StrUtil.isNotBlank(hash)) {
                                return Future.succeededFuture();
                            }
                            return persistCredentials(Config.ADMIN_USERNAME, Config.ADMIN_PASSWORD);
                        }));
    }

    public Future<Boolean> verifyCredentials(String username, String password) {
        return DataVerticle.settingRepository.<String>getByKey(SettingKey.adminUsername)
                .compose(savedUsername -> DataVerticle.settingRepository.<String>getByKey(SettingKey.adminPasswordHash)
                        .map(savedHash -> StrUtil.equals(savedUsername, username) && verifyPassword(password, savedHash)));
    }

    public Future<Void> updateCredentials(String username, String password) {
        if (StrUtil.isBlank(username)) {
            return Future.failedFuture("Username is required");
        }
        if (StrUtil.length(password) < PASSWORD_MIN_LENGTH) {
            return Future.failedFuture("Password must be at least 4 characters");
        }
        return persistCredentials(username.trim(), password);
    }

    public boolean isAuthenticated(Session session) {
        return session != null && Boolean.TRUE.equals(session.get(SESSION_AUTHENTICATED));
    }

    public void markAuthenticated(Session session, String username) {
        session.put(SESSION_AUTHENTICATED, true);
        session.put(SESSION_USERNAME, username);
    }

    public void clearAuthenticated(Session session) {
        if (session != null) {
            session.remove(SESSION_AUTHENTICATED);
            session.remove(SESSION_USERNAME);
        }
    }

    private Future<Void> persistCredentials(String username, String password) {
        String hash = hashPassword(password);
        return DataVerticle.settingRepository.createOrUpdate(SettingKey.adminUsername.name(), username)
                .compose(_ -> DataVerticle.settingRepository.createOrUpdate(SettingKey.adminPasswordHash.name(), hash))
                .mapEmpty();
    }

    private String hashPassword(String password) {
        try {
            byte[] salt = new byte[16];
            secureRandom.nextBytes(salt);
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return "pbkdf2$%d$%s$%s".formatted(
                    PBKDF2_ITERATIONS,
                    Base64.getEncoder().encodeToString(salt),
                    Base64.getEncoder().encodeToString(hash)
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash password", e);
        }
    }

    private boolean verifyPassword(String password, String encodedHash) {
        try {
            String[] parts = encodedHash.split("\\$");
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[3]);
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, expectedHash.length * 8);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] actualHash = factory.generateSecret(spec).getEncoded();
            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (Exception e) {
            return MessageDigest.isEqual(new byte[0], new byte[1]);
        }
    }
}
```

- [ ] **Step 4: 重新运行管理员凭据测试并确认通过**

Run: `.\gradlew.bat test --tests telegram.files.AdminAuthServiceTest`

Expected:
- `BUILD SUCCESSFUL`
- `2 tests completed, 0 failed`

- [ ] **Step 5: 提交基础安全凭据实现**

```bash
git add api/src/main/java/telegram/files/AdminAuthService.java api/src/main/java/telegram/files/Config.java api/src/main/java/telegram/files/repository/SettingKey.java api/src/test/java/telegram/files/AdminAuthServiceTest.java
git commit -m "feat: add secure admin credential storage"
```

### Task 2: 为 HTTP 与 WebSocket 增加统一登录拦截

**Files:**
- Modify: `api/src/main/java/telegram/files/HttpVerticle.java`
- Create: `api/src/test/java/telegram/files/HttpVerticleAuthTest.java`

- [ ] **Step 1: 先写未登录拦截和登录流程的集成失败测试**

```java
package telegram.files;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
public class HttpVerticleAuthTest {

    private HttpClient httpClient;
    private String baseUrl;

    @BeforeEach
    void deployApp(Vertx vertx, VertxTestContext testContext) {
        httpClient = HttpClient.newBuilder()
                .cookieHandler(new CookieManager())
                .build();
        baseUrl = "http://localhost:18080";

        vertx.deployVerticle(new DataVerticle())
                .compose(_ -> vertx.deployVerticle(new HttpVerticle(), new DeploymentOptions().setConfig(new io.vertx.core.json.JsonObject().put("http.port", 18080))))
                .onComplete(testContext.succeedingThenComplete());
    }

    @AfterEach
    void cleanup(Vertx vertx, VertxTestContext testContext) {
        DataVerticleTest.clear(vertx).onComplete(testContext.succeedingThenComplete());
    }

    @Test
    @DisplayName("未登录访问受保护接口返回 401")
    void rejectProtectedRouteWithoutSession(VertxTestContext testContext) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/files"))
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .whenComplete((response, error) -> testContext.verify(() -> {
                    assertEquals(null, error);
                    assertEquals(401, response.statusCode());
                    assertTrue(response.body().contains("Unauthorized"));
                    testContext.completeNow();
                }));
    }

    @Test
    @DisplayName("登录成功后可以读取会话状态并退出")
    void loginSessionAndLogoutFlow(VertxTestContext testContext) {
        String body = """
                {"username":"admin","password":"admin"}
                """;

        HttpRequest loginRequest = HttpRequest.newBuilder(URI.create(baseUrl + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        httpClient.sendAsync(loginRequest, HttpResponse.BodyHandlers.ofString())
                .thenCompose(loginResponse -> {
                    testContext.verify(() -> assertEquals(200, loginResponse.statusCode()));
                    HttpRequest sessionRequest = HttpRequest.newBuilder(URI.create(baseUrl + "/auth/session")).GET().build();
                    return httpClient.sendAsync(sessionRequest, HttpResponse.BodyHandlers.ofString());
                })
                .thenCompose(sessionResponse -> {
                    testContext.verify(() -> {
                        assertEquals(200, sessionResponse.statusCode());
                        assertTrue(sessionResponse.body().contains("\"authenticated\":true"));
                    });
                    HttpRequest logoutRequest = HttpRequest.newBuilder(URI.create(baseUrl + "/auth/logout"))
                            .POST(HttpRequest.BodyPublishers.noBody())
                            .build();
                    return httpClient.sendAsync(logoutRequest, HttpResponse.BodyHandlers.ofString());
                })
                .whenComplete((logoutResponse, error) -> testContext.verify(() -> {
                    assertEquals(null, error);
                    assertEquals(200, logoutResponse.statusCode());
                    testContext.completeNow();
                }));
    }
}
```

- [ ] **Step 2: 运行集成测试确认当前失败**

Run: `.\gradlew.bat test --tests telegram.files.HttpVerticleAuthTest`

Expected:
- `FAILURE: Build failed with an exception.`
- 至少一个断言失败，因为当前 `/files` 仍然返回 `200`

- [ ] **Step 3: 在 HTTP 层接入会话守卫与认证接口**

`api/src/main/java/telegram/files/HttpVerticle.java`

```java
private final AdminAuthService adminAuthService = new AdminAuthService();

@Override
public void start(Promise<Void> startPromise) {
    adminAuthService.ensureInitialized()
            .compose(_ -> initHttpServer())
            .compose(_ -> initTelegramVerticles())
            .compose(_ -> AutomationsHolder.INSTANCE.init())
            .compose(_ -> initAutoDownloadVerticle())
            .compose(_ -> initTransferVerticle())
            .compose(_ -> initPreloadMessageVerticle())
            .compose(_ -> initEventConsumer())
            .onSuccess(startPromise::complete)
            .onFailure(startPromise::fail);
}
```

```java
router.route()
        .handler(sessionHandler)
        .handler(BodyHandler.create())
        .handler(this::handleAdminAuthGuard);

router.post("/auth/login").handler(this::handleAdminLogin);
router.post("/auth/logout").handler(this::handleAdminLogout);
router.get("/auth/session").handler(this::handleAdminSession);
router.post("/auth/credentials").handler(this::handleAdminCredentialsUpdate);
```

```java
private void handleAdminAuthGuard(RoutingContext ctx) {
    String path = ctx.request().path();
    if (Set.of("/", "/health", "/version", "/auth/login", "/auth/session").contains(path)) {
        ctx.next();
        return;
    }
    if (!adminAuthService.isAuthenticated(ctx.session())) {
        ctx.response()
                .setStatusCode(401)
                .putHeader("Content-Type", "application/json")
                .end(JsonObject.of("error", "Unauthorized").encode());
        return;
    }
    ctx.next();
}
```

```java
private void handleAdminLogin(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    String username = body == null ? null : body.getString("username");
    String password = body == null ? null : body.getString("password");
    if (StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
        ctx.response().setStatusCode(400).end(JsonObject.of("error", "Username and password are required").encode());
        return;
    }
    adminAuthService.verifyCredentials(username, password)
            .onSuccess(matches -> {
                if (!matches) {
                    ctx.response().setStatusCode(401).end(JsonObject.of("error", "账号或密码错误").encode());
                    return;
                }
                adminAuthService.markAuthenticated(ctx.session(), username.trim());
                ctx.json(JsonObject.of("authenticated", true, "username", username.trim()));
            })
            .onFailure(ctx::fail);
}

private void handleAdminLogout(RoutingContext ctx) {
    adminAuthService.clearAuthenticated(ctx.session());
    ctx.end();
}

private void handleAdminSession(RoutingContext ctx) {
    boolean authenticated = adminAuthService.isAuthenticated(ctx.session());
    String username = authenticated ? ctx.session().get(AdminAuthService.SESSION_USERNAME) : null;
    ctx.json(JsonObject.of("authenticated", authenticated, "username", username));
}

private void handleAdminCredentialsUpdate(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    String username = body == null ? null : body.getString("username");
    String password = body == null ? null : body.getString("password");
    adminAuthService.updateCredentials(username, password)
            .onSuccess(_ -> {
                adminAuthService.markAuthenticated(ctx.session(), username.trim());
                ctx.end();
            })
            .onFailure(err -> ctx.response()
                    .setStatusCode(400)
                    .end(JsonObject.of("error", err.getMessage()).encode()));
}
```

- [ ] **Step 4: 运行认证集成测试并确认通过**

Run: `.\gradlew.bat test --tests telegram.files.HttpVerticleAuthTest`

Expected:
- `BUILD SUCCESSFUL`
- `2 tests completed, 0 failed`

- [ ] **Step 5: 提交后端会话认证与拦截**

```bash
git add api/src/main/java/telegram/files/HttpVerticle.java api/src/test/java/telegram/files/HttpVerticleAuthTest.java
git commit -m "feat: protect http routes with admin session auth"
```

### Task 3: 前端补认证上下文与统一 401 安全处理

**Files:**
- Create: `web/src/hooks/use-auth.tsx`
- Modify: `web/src/app/layout.tsx`
- Modify: `web/src/lib/api.ts`
- Modify: `web/src/lib/types.ts`
- Modify: `web/src/components/swr-provider.tsx`

- [ ] **Step 1: 先写认证上下文类型和未授权错误流的静态接线**

```ts
export type AuthSession = {
  authenticated: boolean;
  username?: string | null;
};

export type AdminCredentialsInput = {
  username: string;
  password: string;
};
```

```ts
export class UnauthorizedError extends Error {
  constructor(message = "Unauthorized") {
    super(message);
    this.name = "UnauthorizedError";
  }
}
```

- [ ] **Step 2: 先让类型检查失败，确认新增依赖点还不存在**

Run: `npm run typecheck`

Expected:
- `error TS2305` 或 `error TS2307`
- 报错包含 `use-auth`、`AuthSession` 或 `UnauthorizedError`

- [ ] **Step 3: 实现认证上下文和 401 统一处理**

`web/src/lib/api.ts`

```ts
let unauthorizedHandler: (() => void) | null = null;

export function registerUnauthorizedHandler(handler: (() => void) | null) {
  unauthorizedHandler = handler;
}

export class UnauthorizedError extends Error {
  constructor(message = "Unauthorized") {
    super(message);
    this.name = "UnauthorizedError";
  }
}

export async function request<T = any>(
  api: string,
  requestInit?: RequestInit,
): Promise<T> {
  const response = await fetch(`${getApiUrl()}${api}`, {
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...requestInit?.headers,
    },
    ...requestInit,
  });

  const responseText = await response.text();
  const data = responseText ? JSON.parse(responseText) : undefined;

  if (response.status === 401) {
    unauthorizedHandler?.();
    throw new UnauthorizedError(data?.error ?? "Unauthorized");
  }
  if (!response.ok) {
    throw new Error(data?.error ?? `Request failed with status ${response.status}`);
  }
  return data as T;
}
```

`web/src/hooks/use-auth.tsx`

```tsx
"use client";

import React, { createContext, useContext, useEffect, useMemo } from "react";
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
  const { data, isLoading, mutate } = useSWR<AuthSession>("/auth/session", request, {
    revalidateOnFocus: false,
  });

  useEffect(() => {
    registerUnauthorizedHandler(() => {
      void mutate({ authenticated: false, username: null }, false);
      if (pathname !== "/login") {
        router.replace("/login");
      }
    });
    return () => registerUnauthorizedHandler(null);
  }, [mutate, pathname, router]);

  const value = useMemo<AuthContextType>(() => ({
    isLoading,
    session: data,
    authenticated: Boolean(data?.authenticated),
    username: data?.username,
    login: async (input) => {
      await POST("/auth/login", input);
      await mutate();
    },
    logout: async () => {
      await POST("/auth/logout");
      await mutate({ authenticated: false, username: null }, false);
      router.replace("/login");
    },
    refreshSession: async () => await mutate(),
  }), [data, isLoading, mutate, router]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
```

`web/src/app/layout.tsx`

```tsx
import { AuthProvider } from "@/hooks/use-auth";
import { ProtectedShell } from "@/components/protected-shell";

<LocalStorageProvider>
  <ThemeProvider attribute="class" defaultTheme="light" disableTransitionOnChange>
    <SWRProvider>
      <AuthProvider>
        <ProtectedShell>
          <WebSocketProvider>
            <SettingsProvider>
              <TelegramAccountProvider>{children}</TelegramAccountProvider>
            </SettingsProvider>
          </WebSocketProvider>
        </ProtectedShell>
      </AuthProvider>
    </SWRProvider>
    <Toaster />
  </ThemeProvider>
</LocalStorageProvider>
```

`web/src/components/swr-provider.tsx`

```tsx
import { RequestParsedError, UnauthorizedError, request } from "@/lib/api";

if (err instanceof UnauthorizedError) {
  return;
}
```

- [ ] **Step 4: 运行前端类型检查并确认通过**

Run: `npm run typecheck`

Expected:
- `Found 0 errors`

- [ ] **Step 5: 提交前端认证上下文与 401 处理**

```bash
git add web/src/app/layout.tsx web/src/components/swr-provider.tsx web/src/hooks/use-auth.tsx web/src/lib/api.ts web/src/lib/types.ts
git commit -m "feat: add frontend auth session handling"
```

### Task 4: 增加登录页与受保护页面壳层

**Files:**
- Create: `web/src/components/protected-shell.tsx`
- Create: `web/src/components/login-form.tsx`
- Create: `web/src/app/login/page.tsx`

- [ ] **Step 1: 先写登录页和页面守卫的最小组件骨架**

```tsx
"use client";

import { useEffect } from "react";
import { usePathname, useRouter } from "next/navigation";
import { useAuth } from "@/hooks/use-auth";

export function ProtectedShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const { authenticated, isLoading } = useAuth();

  useEffect(() => {
    if (isLoading) return;
    if (!authenticated && pathname !== "/login") {
      router.replace("/login");
    }
    if (authenticated && pathname === "/login") {
      router.replace("/");
    }
  }, [authenticated, isLoading, pathname, router]);

  if (pathname === "/login") return <>{children}</>;
  if (isLoading || !authenticated) {
    return <div className="flex min-h-screen items-center justify-center text-sm text-muted-foreground">Checking session...</div>;
  }
  return <>{children}</>;
}
```

- [ ] **Step 2: 先运行 lint/typecheck，确认登录页尚未接入**

Run: `npm run check`

Expected:
- 报错指出 `login/page.tsx`、`login-form.tsx` 或 `ProtectedShell` 尚未完整实现

- [ ] **Step 3: 实现登录页与表单**

`web/src/components/login-form.tsx`

```tsx
"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useAuth } from "@/hooks/use-auth";
import { useToast } from "@/hooks/use-toast";

export function LoginForm() {
  const router = useRouter();
  const { login } = useAuth();
  const { toast } = useToast();
  const [username, setUsername] = useState("admin");
  const [password, setPassword] = useState("admin");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setIsSubmitting(true);
    setError(null);
    try {
      await login({ username, password });
      toast({ variant: "success", description: "登录成功" });
      router.replace("/");
    } catch (err) {
      setError(err instanceof Error ? err.message : "登录失败");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Card className="w-full max-w-md border-border/60 shadow-lg">
      <CardHeader>
        <CardTitle>管理员登录</CardTitle>
        <CardDescription>登录后才能访问 Telegram Files 的全部功能</CardDescription>
      </CardHeader>
      <CardContent>
        <form className="space-y-4" onSubmit={onSubmit}>
          <div className="space-y-2">
            <Label htmlFor="username">账号</Label>
            <Input id="username" autoComplete="username" value={username} onChange={(e) => setUsername(e.target.value)} />
          </div>
          <div className="space-y-2">
            <Label htmlFor="password">密码</Label>
            <Input id="password" type="password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} />
          </div>
          {error && <p className="text-sm text-destructive">{error}</p>}
          <Button className="w-full" type="submit" disabled={isSubmitting}>
            {isSubmitting ? "登录中..." : "登录"}
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}
```

`web/src/app/login/page.tsx`

```tsx
import { LoginForm } from "@/components/login-form";

export default function LoginPage() {
  return (
    <main className="flex min-h-screen items-center justify-center bg-gradient-to-br from-background via-muted/20 to-background px-4">
      <LoginForm />
    </main>
  );
}
```

- [ ] **Step 4: 运行前端检查并确认登录页编译通过**

Run: `npm run check`

Expected:
- `✔ No ESLint warnings or errors`
- `Found 0 errors`

- [ ] **Step 5: 提交登录页与页面守卫**

```bash
git add web/src/app/login/page.tsx web/src/components/login-form.tsx web/src/components/protected-shell.tsx
git commit -m "feat: add admin login page and protected shell"
```

### Task 5: 在设置中加入账号安全页签，并提供退出登录入口

**Files:**
- Create: `web/src/components/security-settings-form.tsx`
- Modify: `web/src/components/settings-dialog.tsx`
- Modify: `web/src/components/header.tsx`

- [ ] **Step 1: 先写账号安全表单骨架和设置页签接线**

```tsx
"use client";

import { FormEvent, useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { POST } from "@/lib/api";
import { useAuth } from "@/hooks/use-auth";
import { useToast } from "@/hooks/use-toast";

export function SecuritySettingsForm() {
  const { username, refreshSession } = useAuth();
  const { toast } = useToast();
  const [nextUsername, setNextUsername] = useState(username ?? "admin");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (password !== confirmPassword) {
      setError("两次输入的密码不一致");
      return;
    }
    setIsSubmitting(true);
    setError(null);
    try {
      await POST("/auth/credentials", { username: nextUsername, password });
      await refreshSession();
      toast({ variant: "success", description: "账号和密码已更新" });
      setPassword("");
      setConfirmPassword("");
    } catch (err) {
      setError(err instanceof Error ? err.message : "更新失败");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form className="space-y-4" onSubmit={onSubmit}>
      <div className="space-y-2">
        <Label htmlFor="next-username">新账号</Label>
        <Input id="next-username" value={nextUsername} onChange={(e) => setNextUsername(e.target.value)} />
      </div>
      <div className="space-y-2">
        <Label htmlFor="next-password">新密码</Label>
        <Input id="next-password" type="password" autoComplete="new-password" value={password} onChange={(e) => setPassword(e.target.value)} />
      </div>
      <div className="space-y-2">
        <Label htmlFor="confirm-password">确认新密码</Label>
        <Input id="confirm-password" type="password" autoComplete="new-password" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} />
      </div>
      {error && <p className="text-sm text-destructive">{error}</p>}
      <Button type="submit" disabled={isSubmitting}>
        {isSubmitting ? "保存中..." : "保存账号安全设置"}
      </Button>
    </form>
  );
}
```

- [ ] **Step 2: 先运行 lint，确认新组件还未真正挂载**

Run: `npm run lint`

Expected:
- 如果组件未接线，当前步骤只需确认没有语法错误；后续挂载前不要求通过最终交互检查

- [ ] **Step 3: 挂载账号安全页签并加入退出登录按钮**

`web/src/components/settings-dialog.tsx`

```tsx
import { SecuritySettingsForm } from "@/components/security-settings-form";

<TabsList className="min-h-9 justify-start overflow-auto">
  <TabsTrigger value="general">General</TabsTrigger>
  <TabsTrigger value="security">Security</TabsTrigger>
  <TabsTrigger value="statistics">Statistics</TabsTrigger>
  <TabsTrigger value="proxys">Proxys</TabsTrigger>
  <TabsTrigger value="api">API</TabsTrigger>
  <TabsTrigger value="about">About</TabsTrigger>
</TabsList>

<TabsContent value="security" className="overflow-hidden">
  <div className="no-scrollbar flex h-full flex-col overflow-y-scroll">
    <SecuritySettingsForm />
  </div>
</TabsContent>
```

`web/src/components/header.tsx`

```tsx
import { LogOut } from "lucide-react";
import { useAuth } from "@/hooks/use-auth";

const { logout, username } = useAuth();

<TooltipWrapper content={username ? `Signed in as ${username}` : "Logout"}>
  <Button
    variant="ghost"
    size="icon"
    onClick={() => void logout()}
    aria-label="Logout"
  >
    <LogOut className="h-4 w-4" />
  </Button>
</TooltipWrapper>
```

- [ ] **Step 4: 运行前端全量检查并确认通过**

Run: `npm run check`

Expected:
- `✔ No ESLint warnings or errors`
- `Found 0 errors`

- [ ] **Step 5: 提交账号安全页与退出入口**

```bash
git add web/src/components/header.tsx web/src/components/security-settings-form.tsx web/src/components/settings-dialog.tsx
git commit -m "feat: add admin security settings and logout"
```

### Task 6: 文档、环境变量和安全回归验证

**Files:**
- Modify: `.env.example`
- Modify: `README.md`

- [ ] **Step 1: 先补环境变量示例和安全提醒文案**

`.env.example`

```env
# Admin credentials used only when no admin account exists in the database yet.
# Change these defaults before the first startup in environments you do not fully control.
ADMIN_USERNAME=admin
ADMIN_PASSWORD=admin
```

`README.md`

```md
### Admin Login

- The web UI is protected by a single administrator account.
- Default credentials are `admin / admin` on first startup only.
- Change the administrator password immediately after the first login.
```

- [ ] **Step 2: 先运行后端认证相关测试**

Run: `.\gradlew.bat test --tests telegram.files.AdminAuthServiceTest --tests telegram.files.HttpVerticleAuthTest`

Expected:
- `BUILD SUCCESSFUL`
- 认证相关测试全部通过

- [ ] **Step 3: 再运行前端检查**

Run: `npm run check`

Expected:
- `✔ No ESLint warnings or errors`
- `Found 0 errors`

- [ ] **Step 4: 做关键安全冒烟验证**

Run:

```bash
cd api
.\gradlew.bat test --tests telegram.files.AdminAuthServiceTest --tests telegram.files.HttpVerticleAuthTest
cd ..\web
npm run build
```

Manual expectations:
- 未登录打开首页会跳转到 `/login`
- 使用 `admin / admin` 可以登录
- 修改账号密码后，退出再用新账号密码登录成功
- 未登录时刷新 `/files` 页面不会看到业务数据
- 未登录建立 `/ws` 连接会失败

- [ ] **Step 5: 提交文档与最终验证结果**

```bash
git add .env.example README.md
git commit -m "docs: document admin auth defaults and security guidance"
```

## Self-Review Checklist

- 设计文档中的所有要求都已映射到任务：登录页、单管理员模型、初始账号密码、修改账号密码、未登录拦截、HTTP 与 WebSocket 保护、`401` 统一处理、安全提示。
- 计划中没有 `TBD`、`TODO`、`implement later`、`similar to Task N` 之类的占位项。
- 所有新引入的类型名、方法名和文件路径在后续任务中保持一致：`AdminAuthService`、`useAuth`、`ProtectedShell`、`SecuritySettingsForm`、`/auth/login`、`/auth/session`、`/auth/logout`、`/auth/credentials`。
