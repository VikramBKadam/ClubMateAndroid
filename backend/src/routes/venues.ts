import { FastifyInstance } from "fastify";
import { query, withTransaction } from "../db/pool.js";
import { authenticate } from "../plugins/auth.js";
import { sendError } from "../utils/http.js";
import { serializeCompactProfile, UserRow } from "../utils/serializers.js";

type VenueRow = {
  id: string;
  name: string;
  neighborhood: string;
  vibe: string | null;
  music: string | null;
  symbol_name: string | null;
  gradient_hex: string[] | null;
  occupancy: string;
};

function serializeVenue(row: VenueRow) {
  return {
    id: row.id,
    name: row.name,
    neighborhood: row.neighborhood,
    vibe: row.vibe ?? "",
    music: row.music ?? "",
    symbolName: row.symbol_name ?? "building.2.fill",
    gradientHex: row.gradient_hex ?? [],
    occupancy: Number(row.occupancy)
  };
}

export async function venueRoutes(app: FastifyInstance): Promise<void> {
  app.get("/venues", { preHandler: authenticate }, async () => {
    const result = await query<VenueRow>(`
      SELECT v.id, v.name, v.neighborhood, v.vibe, v.music, v.symbol_name, v.gradient_hex,
             count(c.id) FILTER (WHERE c.checked_out_at IS NULL)::text AS occupancy
      FROM venues v
      LEFT JOIN checkins c ON c.venue_id = v.id AND c.checked_out_at IS NULL
      WHERE v.is_active = true
      GROUP BY v.id
      ORDER BY v.name
    `);

    return { venues: result.rows.map(serializeVenue) };
  });

  app.get<{ Params: { id: string } }>(
    "/venues/:id/roster",
    { preHandler: authenticate },
    async (request) => {
      const result = await query<UserRow & { photo_url: string | null }>(
        `
          SELECT u.id, u.name, u.age, u.bio, u.job_title, u.company, u.height,
                 u.interests, u.is_verified, pp.cdn_url AS photo_url
          FROM checkins c
          JOIN users u ON u.id = c.user_id
          LEFT JOIN profile_photos pp ON pp.user_id = u.id AND pp.position = 0
          WHERE c.venue_id = $1
            AND c.checked_out_at IS NULL
            AND u.is_active = true
            AND u.is_flagged = false
            AND u.profile_completed = true
          ORDER BY c.checked_in_at DESC
        `,
        [request.params.id]
      );

      return { roster: result.rows.map(serializeCompactProfile) };
    }
  );

  app.post<{ Params: { id: string } }>(
    "/venues/:id/checkin",
    { preHandler: authenticate },
    async (request, reply) => {
      const venue = await query("SELECT id FROM venues WHERE id = $1 AND is_active = true", [request.params.id]);
      if (venue.rowCount === 0) {
        return sendError(reply, 404, "Venue not found");
      }

      await withTransaction(async (client) => {
        await client.query(
          "UPDATE checkins SET checked_out_at = now() WHERE user_id = $1 AND checked_out_at IS NULL",
          [request.auth.userId]
        );
        await client.query(
          "INSERT INTO checkins(user_id, venue_id) VALUES ($1, $2)",
          [request.auth.userId, request.params.id]
        );
      });

      return { ok: true };
    }
  );

  app.delete<{ Params: { id: string } }>(
    "/venues/:id/checkin",
    { preHandler: authenticate },
    async (request) => {
      await query(
        `
          UPDATE checkins
          SET checked_out_at = now()
          WHERE user_id = $1
            AND venue_id = $2
            AND checked_out_at IS NULL
        `,
        [request.auth.userId, request.params.id]
      );
      return { ok: true };
    }
  );
}
