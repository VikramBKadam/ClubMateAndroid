import { FastifyInstance } from "fastify";
import { query, withTransaction } from "../db/pool.js";
import { authenticate } from "../plugins/auth.js";
import { asString, sendError } from "../utils/http.js";

export async function safetyRoutes(app: FastifyInstance): Promise<void> {
  app.post<{ Body: { blockedUserId?: string } }>(
    "/blocks",
    { preHandler: authenticate },
    async (request, reply) => {
      const blockedUserId = asString(request.body?.blockedUserId);
      if (!blockedUserId) {
        return sendError(reply, 400, "blockedUserId is required");
      }

      await query(
        `
          INSERT INTO blocks(blocker_id, blocked_id)
          VALUES ($1, $2)
          ON CONFLICT DO NOTHING
        `,
        [request.auth.userId, blockedUserId]
      );

      return { ok: true };
    }
  );

  app.post<{
    Body: {
      reportedUserId?: string;
      reason?: string;
      details?: string;
    };
  }>("/reports", { preHandler: authenticate }, async (request, reply) => {
    const reportedUserId = asString(request.body?.reportedUserId);
    const reason = asString(request.body?.reason)?.trim();
    if (!reportedUserId || !reason) {
      return sendError(reply, 400, "reportedUserId and reason are required");
    }

    await withTransaction(async (client) => {
      await client.query(
        `
          INSERT INTO reports(reporter_id, reported_id, reason, details)
          VALUES ($1, $2, $3, $4)
        `,
        [request.auth.userId, reportedUserId, reason, asString(request.body?.details) ?? null]
      );
      await client.query(
        `
          INSERT INTO blocks(blocker_id, blocked_id)
          VALUES ($1, $2)
          ON CONFLICT DO NOTHING
        `,
        [request.auth.userId, reportedUserId]
      );
      await client.query(
        `
          UPDATE users
          SET is_flagged = true
          WHERE id = $1
            AND (
              SELECT count(*)
              FROM reports
              WHERE reported_id = $1
            ) >= 3
        `,
        [reportedUserId]
      );
    });

    return { ok: true };
  });
}
