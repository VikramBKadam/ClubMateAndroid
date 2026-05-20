import { FastifyInstance } from "fastify";
import { DbClient, query, withTransaction } from "../db/pool.js";
import { authenticate } from "../plugins/auth.js";
import { asString, asStringArray, sendError } from "../utils/http.js";
import { PhotoRow, PromptRow, serializeProfile, UserRow } from "../utils/serializers.js";

type PromptInput = {
  id?: string;
  question?: string;
  answer?: string;
};

async function fetchProfile(userId: string) {
  const user = await query<UserRow>(
    `
      SELECT id, phone_e164, name, age, bio, job_title, company, height,
             interests, is_verified, profile_completed
      FROM users
      WHERE id = $1 AND is_active = true
    `,
    [userId]
  );

  const userRow = user.rows[0];
  if (!userRow) {
    return undefined;
  }

  const [prompts, photos] = await Promise.all([
    query<PromptRow>(
      "SELECT id, question, answer, position FROM profile_prompts WHERE user_id = $1 ORDER BY position",
      [userId]
    ),
    query<PhotoRow>(
      "SELECT id, storage_key, cdn_url, position FROM profile_photos WHERE user_id = $1 ORDER BY position",
      [userId]
    )
  ]);

  return serializeProfile(userRow, prompts.rows, photos.rows);
}

async function replacePrompts(client: DbClient, userId: string, prompts: PromptInput[] | undefined): Promise<void> {
  if (!prompts) {
    return;
  }

  await client.query("DELETE FROM profile_prompts WHERE user_id = $1", [userId]);
  for (const [index, prompt] of prompts.slice(0, 3).entries()) {
    if (!prompt.question || !prompt.answer) {
      continue;
    }

    await client.query(
      `
        INSERT INTO profile_prompts(user_id, question, answer, position)
        VALUES ($1, $2, $3, $4)
      `,
      [userId, prompt.question.trim(), prompt.answer.trim(), index]
    );
  }
}

export async function userRoutes(app: FastifyInstance): Promise<void> {
  app.get("/users/me", { preHandler: authenticate }, async (request, reply) => {
    const profile = await fetchProfile(request.auth.userId);
    if (!profile) {
      return sendError(reply, 404, "User not found");
    }

    return { user: profile };
  });

  app.put<{
    Body: {
      name?: string;
      age?: number;
      bio?: string;
      jobTitle?: string;
      company?: string;
      height?: string;
      interests?: unknown;
      prompts?: PromptInput[];
    };
  }>("/users/me", { preHandler: authenticate }, async (request, reply) => {
    const body = request.body ?? {};
    const name = asString(body.name)?.trim();
    const age = typeof body.age === "number" ? body.age : undefined;

    if (!name || !age) {
      return sendError(reply, 400, "name and age are required");
    }

    if (age < 18 || age > 120) {
      return sendError(reply, 400, "age must be between 18 and 120");
    }

    await withTransaction(async (client) => {
      await client.query(
        `
          UPDATE users
          SET name = $2,
              age = $3,
              bio = $4,
              job_title = $5,
              company = $6,
              height = $7,
              interests = $8,
              profile_completed = true
          WHERE id = $1
        `,
        [
          request.auth.userId,
          name,
          age,
          asString(body.bio)?.trim() ?? "",
          asString(body.jobTitle)?.trim() ?? "",
          asString(body.company)?.trim() ?? "",
          asString(body.height)?.trim() ?? "",
          asStringArray(body.interests)
        ]
      );

      await replacePrompts(client, request.auth.userId, body.prompts);
    });

    const profile = await fetchProfile(request.auth.userId);
    return { user: profile };
  });

  app.post<{
    Body: {
      storageKey?: string;
      cdnUrl?: string;
      position?: number;
    };
  }>("/users/me/photos", { preHandler: authenticate }, async (request, reply) => {
    const storageKey = asString(request.body?.storageKey)?.trim();
    const cdnUrl = asString(request.body?.cdnUrl)?.trim();
    const position = typeof request.body?.position === "number" ? request.body.position : 0;

    if (!storageKey || !cdnUrl) {
      return sendError(
        reply,
        400,
        "storageKey and cdnUrl are required until Supabase Storage multipart upload is wired"
      );
    }

    const result = await query<PhotoRow>(
      `
        INSERT INTO profile_photos(user_id, storage_key, cdn_url, position)
        VALUES ($1, $2, $3, $4)
        ON CONFLICT (user_id, position)
        DO UPDATE SET storage_key = EXCLUDED.storage_key, cdn_url = EXCLUDED.cdn_url
        RETURNING id, storage_key, cdn_url, position
      `,
      [request.auth.userId, storageKey, cdnUrl, position]
    );

    return { photo: result.rows[0] };
  });

  app.delete<{ Params: { photoId: string } }>(
    "/users/me/photos/:photoId",
    { preHandler: authenticate },
    async (request) => {
      await query("DELETE FROM profile_photos WHERE user_id = $1 AND id = $2", [
        request.auth.userId,
        request.params.photoId
      ]);
      return { ok: true };
    }
  );

  app.put<{ Body: { order?: string[] } }>(
    "/users/me/photos/order",
    { preHandler: authenticate },
    async (request, reply) => {
      const order = request.body?.order;
      if (!Array.isArray(order)) {
        return sendError(reply, 400, "order must be an array of photo IDs");
      }

      await withTransaction(async (client) => {
        for (const [index, photoId] of order.entries()) {
          await client.query(
            "UPDATE profile_photos SET position = $3 WHERE user_id = $1 AND id = $2",
            [request.auth.userId, photoId, -(index + 1)]
          );
        }

        for (const [index, photoId] of order.entries()) {
          await client.query(
            "UPDATE profile_photos SET position = $3 WHERE user_id = $1 AND id = $2",
            [request.auth.userId, photoId, index]
          );
        }
      });

      return { ok: true };
    }
  );

  app.post<{ Body: { token?: string; platform?: string } }>(
    "/users/me/device-token",
    { preHandler: authenticate },
    async (request, reply) => {
      const token = asString(request.body?.token)?.trim();
      if (!token) {
        return sendError(reply, 400, "token is required");
      }

      await query(
        `
          INSERT INTO device_tokens(user_id, token, platform, updated_at)
          VALUES ($1, $2, $3, now())
          ON CONFLICT (user_id, token)
          DO UPDATE SET platform = EXCLUDED.platform, updated_at = now()
        `,
        [request.auth.userId, token, asString(request.body?.platform) ?? "apns"]
      );

      return { ok: true };
    }
  );
}
