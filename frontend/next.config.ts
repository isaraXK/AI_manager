import type { NextConfig } from "next";

const backend = new URL(process.env.AI_MANAGER_BACKEND_URL || "http://127.0.0.1:8080");

if (backend.protocol !== "http:" && backend.protocol !== "https:") {
  throw new Error("AI_MANAGER_BACKEND_URL must use HTTP or HTTPS");
}

const config: NextConfig = {
  async rewrites() {
    return [{ source: "/api/v1/:path*", destination: `${backend.origin}/api/v1/:path*` }];
  },
};

export default config;
