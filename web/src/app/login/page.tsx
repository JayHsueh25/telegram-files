import { LoginForm } from "@/components/login-form";

export default function LoginPage() {
  return (
    <main className="relative flex min-h-screen items-center justify-center overflow-hidden bg-[radial-gradient(circle_at_top,_hsl(var(--primary)/0.18),_transparent_42%),linear-gradient(160deg,_hsl(var(--background)),_hsl(var(--muted)/0.42))] px-4 py-10">
      <div className="absolute inset-0 bg-[linear-gradient(135deg,transparent_0%,hsl(var(--foreground)/0.02)_50%,transparent_100%)]" />
      <div className="relative w-full max-w-md">
        <LoginForm />
      </div>
    </main>
  );
}
