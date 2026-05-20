import "dotenv/config";

export type AppEnv = {
  nodeEnv: string;
  host: string;
  port: number;
  databaseUrl: string;
  databaseSsl: boolean;
  jwtSecret: string;
  accessTokenTtlSeconds: number;
  refreshTokenDays: number;
  devOtpCode?: string;
  corsOrigin: string[];
};

function required(name: string): string {
  const value = process.env[name];
  if (!value || value.trim().length === 0) {
    throw new Error(`Missing required environment variable ${name}`);
  }
  return value;
}

function numberEnv(name: string, fallback: number): number {
  const value = process.env[name];
  if (!value) { return fallback; }

  const parsed = Number(value);
  if (!Number.isFinite(parsed)) {
    throw new Error(`Environment variable ${name} must be a number`);
  }
  return parsed;
}

function optionalSecret(name: string, fallback: string): string {
  const value = process.env[name];
  if (value) { return value; }

  if (process.env.NODE_ENV === "production") {
    throw new Error(`Missing required production secret ${name}`);
  }

  return fallback;
}

export const env: AppEnv = {
  nodeEnv: process.env.NODE_ENV ?? "development",
  host: process.env.HOST ?? "0.0.0.0",
  port: numberEnv("PORT", 3000),
  databaseUrl: required("DATABASE_URL"),
  databaseSsl: process.env.DATABASE_SSL !== "false",
  jwtSecret: optionalSecret("JWT_SECRET", "dev-only-change-me"),
  accessTokenTtlSeconds: numberEnv("ACCESS_TOKEN_TTL_SECONDS", 900),
  refreshTokenDays: numberEnv("REFRESH_TOKEN_DAYS", 30),
  devOtpCode: process.env.DEV_OTP_CODE,
  corsOrigin: (process.env.CORS_ORIGIN ?? "*")
    .split(",")
    .map((origin) => origin.trim())
    .filter(Boolean)
};
