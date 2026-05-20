import { FastifyInstance } from "fastify";
import { env } from "../config/env.js";
import { query, withTransaction } from "../db/pool.js";
import { hashValue, randomOtp, randomToken } from "../utils/crypto.js";
import { asString, sendError } from "../utils/http.js";
import { assertValidPhone, normalizePhone } from "../utils/phone.js";
import { signAccessToken } from "../utils/auth.js";

type AuthUserRow = {
  id: string;
  phone_e164: string;
  profile_completed: boolean;
};

async function persistRefreshToken(userId: string): Promise<string> {
  const refreshToken = randomToken(48);
  const tokenHash = hashValue(refreshToken);
  await query(
    `
      INSERT INTO refresh_tokens(token_hash, user_id, expires_at)
      VALUES ($1, $2, now() + ($3::text || ' days')::interval)
    `,
    [tokenHash, userId, env.refreshTokenDays]
  );
  return refreshToken;
}

async function sessionPayload(user: AuthUserRow) {
  const accessToken = signAccessToken({
    userId: user.id,
    phone: user.phone_e164
  });
  const refreshToken = await persistRefreshToken(user.id);

  return {
    accessToken,
    refreshToken,
    user: {
      id: user.id,
      phone: user.phone_e164,
      profileCompleted: user.profile_completed
    }
  };
}

export async function authRoutes(app: FastifyInstance): Promise<void> {
  app.post<{ Body: { phone?: string } }>("/auth/otp/send", async (request, reply) => {
    const rawPhone = asString(request.body?.phone);
    if (!rawPhone) {
      return sendError(reply, 400, "phone is required");
    }

    const phone = normalizePhone(rawPhone);
    try {
      assertValidPhone(phone);
    } catch (error) {
      return sendError(reply, 400, error instanceof Error ? error.message : "Invalid phone");
    }

    const code = env.devOtpCode ?? randomOtp();
    await query(
      `
        INSERT INTO otp_codes(phone_e164, code_hash, expires_at, attempts)
        VALUES ($1, $2, now() + interval '5 minutes', 0)
        ON CONFLICT (phone_e164)
        DO UPDATE SET code_hash = EXCLUDED.code_hash,
                      expires_at = EXCLUDED.expires_at,
                      attempts = 0,
                      created_at = now()
      `,
      [phone, hashValue(`${phone}:${code}`)]
    );

    // MSG91/Twilio integration goes here. In development we return the code for local testing.
    return {
      ok: true,
      ...(env.nodeEnv !== "production" ? { devCode: code } : {})
    };
  });

  app.post<{ Body: { phone?: string; code?: string } }>("/auth/otp/verify", async (request, reply) => {
    const rawPhone = asString(request.body?.phone);
    const rawCode = asString(request.body?.code);
    if (!rawPhone || !rawCode) {
      return sendError(reply, 400, "phone and code are required");
    }

    const phone = normalizePhone(rawPhone);
    const code = rawCode.trim();

    const otp = await query<{
      code_hash: string;
      expires_at: Date;
      attempts: number;
    }>(
      "SELECT code_hash, expires_at, attempts FROM otp_codes WHERE phone_e164 = $1",
      [phone]
    );

    const row = otp.rows[0];
    if (!row || row.expires_at.getTime() < Date.now() || row.attempts >= 5) {
      return sendError(reply, 401, "OTP is invalid or expired");
    }

    if (row.code_hash !== hashValue(`${phone}:${code}`)) {
      await query("UPDATE otp_codes SET attempts = attempts + 1 WHERE phone_e164 = $1", [phone]);
      return sendError(reply, 401, "OTP is invalid or expired");
    }

    const user = await withTransaction<AuthUserRow>(async (client) => {
      await client.query("DELETE FROM otp_codes WHERE phone_e164 = $1", [phone]);
      const result = await client.query<AuthUserRow>(
        `
          INSERT INTO users(phone_e164)
          VALUES ($1)
          ON CONFLICT (phone_e164)
          DO UPDATE SET updated_at = now()
          RETURNING id, phone_e164, profile_completed
        `,
        [phone]
      );
      return result.rows[0];
    });

    const payload = await sessionPayload(user);
    return {
      ...payload,
      isNewUser: !user.profile_completed
    };
  });

  app.post<{ Body: { refreshToken?: string } }>("/auth/refresh", async (request, reply) => {
    const refreshToken = asString(request.body?.refreshToken);
    if (!refreshToken) {
      return sendError(reply, 400, "refreshToken is required");
    }

    const tokenHash = hashValue(refreshToken);
    const user = await withTransaction<AuthUserRow | undefined>(async (client) => {
      const token = await client.query<{ user_id: string }>(
        `
          SELECT user_id
          FROM refresh_tokens
          WHERE token_hash = $1
            AND revoked_at IS NULL
            AND expires_at > now()
          FOR UPDATE
        `,
        [tokenHash]
      );
      const tokenRow = token.rows[0];
      if (!tokenRow) {
        return undefined;
      }

      await client.query("UPDATE refresh_tokens SET revoked_at = now() WHERE token_hash = $1", [tokenHash]);
      const userResult = await client.query<AuthUserRow>(
        "SELECT id, phone_e164, profile_completed FROM users WHERE id = $1 AND is_active = true",
        [tokenRow.user_id]
      );
      return userResult.rows[0];
    });

    if (!user) {
      return sendError(reply, 401, "Refresh token is invalid or expired");
    }

    return sessionPayload(user);
  });

  app.delete<{ Body: { refreshToken?: string } }>("/auth/session", async (request) => {
    const refreshToken = asString(request.body?.refreshToken);
    if (refreshToken) {
      await query("UPDATE refresh_tokens SET revoked_at = now() WHERE token_hash = $1", [
        hashValue(refreshToken)
      ]);
    }
    return { ok: true };
  });
}
