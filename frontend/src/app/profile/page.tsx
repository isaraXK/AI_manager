"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState, type FormEvent } from "react";
import { AccountFrame, LoadError } from "../../features/auth/account-frame";
import {
  ApiError, errorMessage, getProfile, saveProfile, type ProfileBasics, type ProfileStatus,
} from "../../lib/account-api";

const emptyFields: ProfileBasics = { nickname: "", gender: "", birthDate: "" };

export default function ProfilePage() {
  const router = useRouter();
  const [current, setCurrent] = useState<ProfileStatus | null>(null);
  const [fields, setFields] = useState<ProfileBasics>(emptyFields);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [error, setError] = useState("");
  const [attempt, setAttempt] = useState(0);
  const [saving, setSaving] = useState(false);
  const [conflict, setConflict] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [latest, setLatest] = useState<ProfileStatus | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    getProfile(controller.signal)
      .then((result) => {
        if (controller.signal.aborted) return;
        setCurrent(result);
        if (result.profile) {
          const { nickname, gender, birthDate } = result.profile;
          setFields({ nickname, gender, birthDate });
        }
      })
      .catch((failure: unknown) => {
        if (controller.signal.aborted) return;
        if (failure instanceof ApiError && failure.status === 401) {
          router.replace("/login");
        } else {
          setError(errorMessage(failure));
        }
      });
    return () => controller.abort();
  }, [router, attempt]);

  function change(field: keyof ProfileBasics, value: string) {
    setFields((previous) => ({ ...previous, [field]: value }));
    setFieldErrors((previous) => ({ ...previous, [field]: "" }));
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!current || saving || conflict) return;
    setSaving(true);
    setError("");
    setFieldErrors({});
    try {
      await saveProfile(current, fields);
      router.replace("/");
    } catch (failure) {
      if (failure instanceof ApiError) {
        if (failure.status === 401) {
          router.replace("/login");
          return;
        }
        if (failure.status === 409) setConflict(true);
        if (failure.status === 422) setFieldErrors(failure.details);
      }
      setError(errorMessage(failure));
      setSaving(false);
    }
  }

  async function readLatest() {
    if (refreshing) return;
    setRefreshing(true);
    setError("");
    try {
      const result = await getProfile();
      setCurrent(result);
      setLatest(result);
      setConflict(false);
    } catch (failure) {
      if (failure instanceof ApiError && failure.status === 401) {
        router.replace("/login");
      } else {
        setError(errorMessage(failure));
      }
    } finally {
      setRefreshing(false);
    }
  }

  if (!current) {
    return (
      <AccountFrame title="个人资料">
        {error ? <LoadError message={error} onRetry={() => { setError(""); setAttempt(attempt + 1); }} /> : (
          <p className="muted" role="status">正在读取个人资料…</p>
        )}
      </AccountFrame>
    );
  }

  return (
    <AccountFrame
      title={current.profileReady ? "编辑个人资料" : "先认识一下你"}
      description={current.profileReady ? "在这里更新你的基本资料。" : "填好称呼、性别和生日，就可以进入首页。"}
    >
      <form className="form-stack" method="post" onSubmit={submit}>
        {latest && !conflict && (
          <div className="notice" role="status">
            <p>已读取最新版本，本次输入仍保留。核对后再保存。</p>
            {latest.profile && (
              <p>当前记录：{latest.profile.nickname} · {latest.profile.gender} · {latest.profile.birthDate}</p>
            )}
          </div>
        )}
        <div className="form-field">
          <label htmlFor="nickname">称呼</label>
          <input id="nickname" name="nickname" maxLength={100} value={fields.nickname} required
            onChange={(event) => change("nickname", event.target.value)}
            aria-invalid={Boolean(fieldErrors.nickname)}
            aria-describedby={fieldErrors.nickname ? "nickname-error" : undefined} />
          {fieldErrors.nickname && <p id="nickname-error" className="field-error">{fieldErrors.nickname}</p>}
        </div>
        <div className="form-field">
          <label htmlFor="gender">性别</label>
          <input id="gender" name="gender" maxLength={32} value={fields.gender} required
            onChange={(event) => change("gender", event.target.value)}
            aria-invalid={Boolean(fieldErrors.gender)}
            aria-describedby={fieldErrors.gender ? "gender-error" : undefined} />
          {fieldErrors.gender && <p id="gender-error" className="field-error">{fieldErrors.gender}</p>}
        </div>
        <div className="form-field">
          <label htmlFor="birthDate">生日</label>
          <input id="birthDate" name="birthDate" type="date" min="1000-01-01" value={fields.birthDate} required
            onChange={(event) => change("birthDate", event.target.value)}
            aria-invalid={Boolean(fieldErrors.birthDate)}
            aria-describedby={fieldErrors.birthDate ? "birthday-help birthdate-error" : "birthday-help"} />
          <p id="birthday-help" className="muted field-help">年龄会根据生日自动计算。</p>
          {fieldErrors.birthDate && <p id="birthdate-error" className="field-error">{fieldErrors.birthDate}</p>}
        </div>
        {error && <p className="notice notice-error" role="alert">{error}</p>}
        {conflict && (
          <button className="button-secondary" type="button" onClick={readLatest} disabled={refreshing}>
            {refreshing ? "正在读取…" : "读取最新资料"}
          </button>
        )}
        <div className="actions">
          <button className="button-primary" type="submit" disabled={saving || conflict}>
            {saving ? "保存中…" : "保存资料"}
          </button>
          {current.profileReady && <Link className="button-secondary" href="/">返回首页</Link>}
        </div>
      </form>
    </AccountFrame>
  );
}
