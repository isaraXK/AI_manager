export type SessionStatus = {
  loggedIn: boolean;
  profileReady: boolean;
};

export type ProfileBasics = {
  nickname: string;
  gender: string;
  birthDate: string;
};

type ProfileFields = ProfileBasics & {
  occupation?: string | null;
  routine?: string | null;
  interests?: string | null;
  mbti?: string | null;
  zodiac?: string | null;
  bloodType?: string | null;
  selfIntro?: string | null;
};

export type ProfileStatus = {
  profileReady: boolean;
  version: number;
  profile: ProfileFields | null;
  age: number | null;
  updatedAt: string | null;
};

type ErrorBody = {
  code?: string;
  message?: string;
  details?: Record<string, string>;
};

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string | undefined,
    message: string,
    public readonly details: Record<string, string>,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

async function request(path: string, init: RequestInit = {}): Promise<Response> {
  const response = await fetch(`/api/v1${path}`, {
    ...init,
    credentials: "same-origin",
    cache: "no-store",
  });
  if (!response.ok) {
    const error: ErrorBody | null = await response.json().catch(() => null);
    throw new ApiError(
      response.status,
      error?.code,
      error?.code && error.message ? error.message : "暂时无法完成请求，请稍后重试。",
      error?.details ?? {},
    );
  }
  return response;
}

async function json<T>(path: string, init?: RequestInit): Promise<T> {
  return (await request(path, init)).json() as Promise<T>;
}

function write(method: "POST" | "PUT", body?: object): RequestInit {
  const headers: Record<string, string> = { "X-AI-Manager-Request": "1" };
  if (body === undefined) {
    return { method, headers };
  }
  headers["Content-Type"] = "application/json";
  return { method, headers, body: JSON.stringify(body) };
}

export function getSession(signal?: AbortSignal) {
  return json<SessionStatus>("/auth/session", { signal });
}

export function login(username: string, password: string) {
  return json<SessionStatus>("/auth/login", write("POST", { username, password }));
}

export async function logout() {
  await request("/auth/logout", write("POST"));
}

export function getProfile(signal?: AbortSignal) {
  return json<ProfileStatus>("/profile", { signal });
}

export function saveProfile(current: ProfileStatus, basics: ProfileBasics) {
  return json<ProfileStatus>(
    "/profile",
    write("PUT", { version: current.version, profile: { ...current.profile, ...basics } }),
  );
}

export function errorMessage(error: unknown) {
  return error instanceof ApiError ? error.message : "暂时无法连接管家，请稍后重试。";
}
