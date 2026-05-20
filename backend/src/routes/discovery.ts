import { FastifyInstance } from "fastify";
import { query } from "../db/pool.js";
import { authenticate } from "../plugins/auth.js";
import { asString, sendError } from "../utils/http.js";
import { serializeCompactProfile, UserRow } from "../utils/serializers.js";

type SwipeDirection = "like" | "nope" | "superlike";

function isSwipeDirection(value: unknown): value is SwipeDirection {
  return value === "like" || value === "nope" || value === "superlike";
}

export async function discoveryRoutes(app: FastifyInstance): Promise<void> {
  app.get<{ Params: { id: string } }>(
    "/venues/:id/discover",
    { preHandler: authenticate },
    async (request) => {
      const result = await query<UserRow & { photo_url: string | null; has_super_liked_me: boolean }>(
        `
          SELECT u.id, u.name, u.age, u.bio, u.job_title, u.company, u.height,
                 u.interests, u.is_verified, pp.cdn_url AS photo_url,
                 (s.direction = 'superlike') AS has_super_liked_me
          FROM users u
          JOIN checkins c ON c.user_id = u.id
            AND c.venue_id = $1
            AND c.checked_out_at IS NULL
          LEFT JOIN profile_photos pp ON pp.user_id = u.id AND pp.position = 0
          LEFT JOIN swipes s ON s.actor_id = u.id
            AND s.target_id = $2
            AND s.venue_id = $1
            AND s.direction = 'superlike'
          WHERE u.id != $2
            AND u.is_active = true
            AND u.is_flagged = false
            AND u.profile_completed = true
            AND NOT EXISTS (
              SELECT 1 FROM swipes
              WHERE actor_id = $2 AND target_id = u.id AND venue_id = $1
            )
            AND NOT EXISTS (
              SELECT 1 FROM blocks
              WHERE (blocker_id = $2 AND blocked_id = u.id)
                 OR (blocker_id = u.id AND blocked_id = $2)
            )
          ORDER BY c.checked_in_at DESC
          LIMIT 20
        `,
        [request.params.id, request.auth.userId]
      );

      return { profiles: result.rows.map(serializeCompactProfile) };
    }
  );

  app.post<{
    Body: {
      targetUserId?: string;
      venueId?: string;
      direction?: string;
    };
  }>("/swipes", { preHandler: authenticate }, async (request, reply) => {
    const targetUserId = asString(request.body?.targetUserId);
    const venueId = asString(request.body?.venueId);
    const direction = request.body?.direction;

    if (!targetUserId || !venueId || !isSwipeDirection(direction)) {
      return sendError(reply, 400, "targetUserId, venueId, and valid direction are required");
    }

    const swipe = await query<{ is_match: boolean; match_id: string | null }>(
      "SELECT * FROM record_swipe($1, $2, $3, $4)",
      [request.auth.userId, targetUserId, venueId, direction]
    );
    const row = swipe.rows[0];

    return {
      isMatch: row?.is_match ?? false,
      matchId: row?.match_id ?? null
    };
  });

  app.delete<{ Params: { targetUserId: string; venueId: string } }>(
    "/swipes/:targetUserId/:venueId",
    { preHandler: authenticate },
    async (request) => {
      await query(
        "DELETE FROM swipes WHERE actor_id = $1 AND target_id = $2 AND venue_id = $3",
        [request.auth.userId, request.params.targetUserId, request.params.venueId]
      );
      return { ok: true };
    }
  );
}
