import { FastifyInstance } from "fastify";
import { query } from "../db/pool.js";
import { authenticate } from "../plugins/auth.js";
import { asString, sendError } from "../utils/http.js";
import { serializeCompactProfile, UserRow } from "../utils/serializers.js";

type MatchRow = UserRow & {
  match_id: string;
  venue_id: string | null;
  matched_at: Date;
  photo_url: string | null;
  last_message: string | null;
  last_message_at: Date | null;
  unread_count: string;
};

type MessageRow = {
  id: string;
  match_id: string;
  sender_id: string;
  body: string;
  is_read: boolean;
  created_at: Date;
};

async function userCanAccessMatch(userId: string, matchId: string): Promise<boolean> {
  const result = await query(
    `
      SELECT 1
      FROM matches
      WHERE id = $1
        AND unmatched_at IS NULL
        AND (user_a_id = $2 OR user_b_id = $2)
    `,
    [matchId, userId]
  );
  return (result.rowCount ?? 0) > 0;
}

function serializeMessage(row: MessageRow, myUserId: string) {
  return {
    id: row.id,
    matchId: row.match_id,
    body: row.body,
    isFromMe: row.sender_id === myUserId,
    senderId: row.sender_id,
    isRead: row.is_read,
    createdAt: row.created_at.toISOString()
  };
}

export async function matchRoutes(app: FastifyInstance): Promise<void> {
  app.get("/matches", { preHandler: authenticate }, async (request) => {
    const result = await query<MatchRow>(
      `
        SELECT m.id AS match_id, m.venue_id, m.created_at AS matched_at,
               u.id, u.name, u.age, u.bio, u.job_title, u.company, u.height,
               u.interests, u.is_verified, pp.cdn_url AS photo_url,
               lm.body AS last_message,
               lm.created_at AS last_message_at,
               count(unread.id)::text AS unread_count
        FROM matches m
        JOIN users u ON u.id = CASE
          WHEN m.user_a_id = $1 THEN m.user_b_id
          ELSE m.user_a_id
        END
        LEFT JOIN profile_photos pp ON pp.user_id = u.id AND pp.position = 0
        LEFT JOIN LATERAL (
          SELECT body, created_at
          FROM messages
          WHERE match_id = m.id
          ORDER BY created_at DESC
          LIMIT 1
        ) lm ON true
        LEFT JOIN messages unread ON unread.match_id = m.id
          AND unread.sender_id != $1
          AND unread.is_read = false
        WHERE (m.user_a_id = $1 OR m.user_b_id = $1)
          AND m.unmatched_at IS NULL
        GROUP BY m.id, u.id, pp.cdn_url, lm.body, lm.created_at
        ORDER BY COALESCE(lm.created_at, m.created_at) DESC
      `,
      [request.auth.userId]
    );

    return {
      matches: result.rows.map((row) => ({
        id: row.match_id,
        venueId: row.venue_id,
        profile: serializeCompactProfile(row),
        lastMessage: row.last_message ?? "It's a match!",
        lastMessageAt: (row.last_message_at ?? row.matched_at).toISOString(),
        unreadCount: Number(row.unread_count),
        isNew: row.last_message === null
      }))
    };
  });

  app.get<{
    Params: { id: string };
    Querystring: { before?: string; limit?: string };
  }>("/matches/:id/messages", { preHandler: authenticate }, async (request, reply) => {
    if (!(await userCanAccessMatch(request.auth.userId, request.params.id))) {
      return sendError(reply, 404, "Match not found");
    }

    const limit = Math.min(Number(request.query.limit ?? 50), 100);
    const before = request.query.before ? new Date(request.query.before) : undefined;
    const result = await query<MessageRow>(
      `
        SELECT id, match_id, sender_id, body, is_read, created_at
        FROM messages
        WHERE match_id = $1
          AND ($2::timestamptz IS NULL OR created_at < $2)
        ORDER BY created_at ASC
        LIMIT $3
      `,
      [request.params.id, before ?? null, limit]
    );

    return { messages: result.rows.map((row) => serializeMessage(row, request.auth.userId)) };
  });

  app.post<{
    Params: { id: string };
    Body: { body?: string };
  }>("/matches/:id/messages", { preHandler: authenticate }, async (request, reply) => {
    if (!(await userCanAccessMatch(request.auth.userId, request.params.id))) {
      return sendError(reply, 404, "Match not found");
    }

    const body = asString(request.body?.body)?.trim();
    if (!body) {
      return sendError(reply, 400, "body is required");
    }

    const result = await query<MessageRow>(
      `
        INSERT INTO messages(match_id, sender_id, body, is_read)
        VALUES ($1, $2, $3, false)
        RETURNING id, match_id, sender_id, body, is_read, created_at
      `,
      [request.params.id, request.auth.userId, body]
    );

    return { message: serializeMessage(result.rows[0], request.auth.userId) };
  });

  app.put<{
    Params: { id: string };
    Body: { upToMessageId?: string };
  }>("/matches/:id/messages/read", { preHandler: authenticate }, async (request, reply) => {
    if (!(await userCanAccessMatch(request.auth.userId, request.params.id))) {
      return sendError(reply, 404, "Match not found");
    }

    const upToMessageId = asString(request.body?.upToMessageId);
    if (upToMessageId) {
      await query(
        `
          UPDATE messages
          SET is_read = true
          WHERE match_id = $1
            AND sender_id != $2
            AND created_at <= (
              SELECT created_at FROM messages WHERE id = $3 AND match_id = $1
            )
        `,
        [request.params.id, request.auth.userId, upToMessageId]
      );
    } else {
      await query(
        "UPDATE messages SET is_read = true WHERE match_id = $1 AND sender_id != $2",
        [request.params.id, request.auth.userId]
      );
    }

    return { ok: true };
  });

  app.delete<{ Params: { id: string } }>(
    "/matches/:id",
    { preHandler: authenticate },
    async (request, reply) => {
      const result = await query(
        "DELETE FROM matches WHERE id = $1 AND (user_a_id = $2 OR user_b_id = $2)",
        [request.params.id, request.auth.userId]
      );
      if (result.rowCount === 0) {
        return sendError(reply, 404, "Match not found");
      }
      return { ok: true };
    }
  );
}
