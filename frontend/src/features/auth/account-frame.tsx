import type { ReactNode } from "react";

export function AccountFrame({
  title,
  description,
  children,
}: {
  title: string;
  description?: string;
  children: ReactNode;
}) {
  return (
    <main className="account-shell">
      <div className="account-panel">
        <div className="brand">
          <span className="brand-mark" aria-hidden="true">管</span>
          <span>AI 管家</span>
        </div>
        <header className="panel-heading">
          <h1>{title}</h1>
          {description && <p>{description}</p>}
        </header>
        {children}
      </div>
    </main>
  );
}

export function LoadError({ message, onRetry }: { message: string; onRetry: () => void }) {
  return (
    <div className="form-stack">
      <p className="notice notice-error" role="alert">{message}</p>
      <button className="button-secondary" type="button" onClick={onRetry}>重试</button>
    </div>
  );
}
