"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState, type FormEvent } from "react";
import { AccountFrame } from "../../features/auth/account-frame";
import { errorMessage, getSession, login } from "../../lib/account-api";

export default function LoginPage() {
  const router = useRouter();
  const [checking, setChecking] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    getSession(controller.signal)
      .then((session) => {
        if (controller.signal.aborted) return;
        if (session.loggedIn) {
          router.replace(session.profileReady ? "/" : "/profile");
        } else {
          setChecking(false);
        }
      })
      .catch((failure: unknown) => {
        if (controller.signal.aborted) return;
        setError(errorMessage(failure));
        setChecking(false);
      });
    return () => controller.abort();
  }, [router]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting) return;
    const data = new FormData(event.currentTarget);
    setSubmitting(true);
    setError("");
    try {
      const session = await login(String(data.get("username") ?? "").trim(), String(data.get("password") ?? ""));
      router.replace(session.profileReady ? "/" : "/profile");
    } catch (failure) {
      setError(errorMessage(failure));
      setSubmitting(false);
    }
  }

  return (
    <AccountFrame title="欢迎回来" description="登录后继续记录你的日常。">
      {checking ? (
        <p className="muted" role="status">正在检查登录状态…</p>
      ) : (
        <form className="form-stack" method="post" onSubmit={submit}>
          <label className="form-field" htmlFor="username">
            <span>用户名</span>
            <input id="username" name="username" autoComplete="username" maxLength={100} required />
          </label>
          <label className="form-field" htmlFor="password">
            <span>密码</span>
            <input id="password" name="password" type="password" autoComplete="current-password" maxLength={256} required />
          </label>
          {error && <p className="notice notice-error" role="alert">{error}</p>}
          <button className="button-primary" type="submit" disabled={submitting}>
            {submitting ? "登录中…" : "登录"}
          </button>
        </form>
      )}
    </AccountFrame>
  );
}
