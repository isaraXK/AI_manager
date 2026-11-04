"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { AccountFrame, LoadError } from "../features/auth/account-frame";
import { ApiError, errorMessage, getProfile, getSession, logout, type ProfileStatus } from "../lib/account-api";

export default function Home() {
  const router = useRouter();
  const [profileStatus, setProfileStatus] = useState<ProfileStatus | null>(null);
  const [error, setError] = useState("");
  const [attempt, setAttempt] = useState(0);
  const [leaving, setLeaving] = useState(false);

  useEffect(() => {
    const controller = new AbortController();
    async function load() {
      try {
        const session = await getSession(controller.signal);
        if (controller.signal.aborted) return;
        if (!session.loggedIn) {
          router.replace("/login");
          return;
        }
        if (!session.profileReady) {
          router.replace("/profile");
          return;
        }
        const current = await getProfile(controller.signal);
        if (controller.signal.aborted) return;
        if (current.profile) {
          setProfileStatus(current);
        } else {
          router.replace("/profile");
        }
      } catch (failure) {
        if (controller.signal.aborted) return;
        if (failure instanceof ApiError && failure.status === 401) {
          router.replace("/login");
        } else {
          setError(errorMessage(failure));
        }
      }
    }
    void load();
    return () => controller.abort();
  }, [router, attempt]);

  async function signOut() {
    if (leaving) return;
    setLeaving(true);
    setError("");
    try {
      await logout();
      router.replace("/login");
    } catch (failure) {
      setError(errorMessage(failure));
      setLeaving(false);
    }
  }

  const profile = profileStatus?.profile;
  if (!profile) {
    return (
      <AccountFrame title="欢迎来到 AI 管家">
        {error ? <LoadError message={error} onRetry={() => { setError(""); setAttempt(attempt + 1); }} /> : (
          <p className="muted" role="status">正在打开你的空间…</p>
        )}
      </AccountFrame>
    );
  }

  return (
    <AccountFrame title={`你好，${profile.nickname}`} description="欢迎回到你的日常。">
      <section className="profile-summary" aria-labelledby="profile-summary-title">
        <h2 id="profile-summary-title">个人资料</h2>
        <dl>
          <div><dt>称呼</dt><dd>{profile.nickname}</dd></div>
          <div><dt>性别</dt><dd>{profile.gender}</dd></div>
          <div><dt>生日</dt><dd>{profile.birthDate}</dd></div>
          <div><dt>年龄</dt><dd>{profileStatus?.age ?? "—"}</dd></div>
        </dl>
      </section>
      {error && <p className="notice notice-error" role="alert">{error}</p>}
      <div className="actions">
        <Link className="button-primary" href="/profile">编辑个人资料</Link>
        <button className="button-secondary" type="button" onClick={signOut} disabled={leaving}>
          {leaving ? "退出中…" : "退出登录"}
        </button>
      </div>
    </AccountFrame>
  );
}
