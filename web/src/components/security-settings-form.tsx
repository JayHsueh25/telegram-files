"use client";

import { type FormEvent, useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useAuth } from "@/hooks/use-auth";
import { useToast } from "@/hooks/use-toast";
import { POST } from "@/lib/api";

export function SecuritySettingsForm() {
  const { username, refreshSession } = useAuth();
  const { toast } = useToast();
  const [nextUsername, setNextUsername] = useState(username ?? "admin");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    setNextUsername(username ?? "admin");
  }, [username]);

  const onSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);

    if (password !== confirmPassword) {
      setError("两次输入的密码不一致。");
      return;
    }

    setIsSubmitting(true);
    try {
      await POST("/auth/credentials", {
        username: nextUsername,
        password,
      });
      await refreshSession();
      setPassword("");
      setConfirmPassword("");
      toast({
        variant: "success",
        description: "账号与密码已更新。",
      });
    } catch (submissionError) {
      setError(
        submissionError instanceof Error
          ? submissionError.message
          : "更新失败，请稍后重试。",
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="space-y-6 rounded-xl border border-border/70 bg-card/80 p-4">
      <div className="space-y-2">
        <h3 className="text-lg font-semibold">账号安全</h3>
        <p className="text-sm leading-6 text-muted-foreground">
          当前登录账号为
          <span className="font-medium text-foreground">
            {" "}
            {username ?? "admin"}
          </span>
          。修改后会立即覆盖旧账号和旧密码。
        </p>
      </div>

      <form className="space-y-4" onSubmit={onSubmit}>
        <div className="space-y-2">
          <Label htmlFor="security-username">新账号</Label>
          <Input
            id="security-username"
            autoComplete="username"
            value={nextUsername}
            onChange={(event) => setNextUsername(event.target.value)}
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor="security-password">新密码</Label>
          <Input
            id="security-password"
            type="password"
            autoComplete="new-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor="security-confirm-password">确认新密码</Label>
          <Input
            id="security-confirm-password"
            type="password"
            autoComplete="new-password"
            value={confirmPassword}
            onChange={(event) => setConfirmPassword(event.target.value)}
          />
        </div>
        {error ? (
          <p className="rounded-md border border-destructive/25 bg-destructive/10 px-3 py-2 text-sm text-destructive">
            {error}
          </p>
        ) : null}
        <Button disabled={isSubmitting} type="submit">
          {isSubmitting ? "保存中..." : "保存账号安全设置"}
        </Button>
      </form>
    </div>
  );
}
