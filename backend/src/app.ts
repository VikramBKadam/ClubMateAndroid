import cors from "@fastify/cors";
import multipart from "@fastify/multipart";
import websocket from "@fastify/websocket";
import Fastify from "fastify";
import { env } from "./config/env.js";
import { authRoutes } from "./routes/auth.js";
import { discoveryRoutes } from "./routes/discovery.js";
import { healthRoutes } from "./routes/health.js";
import { matchRoutes } from "./routes/matches.js";
import { safetyRoutes } from "./routes/safety.js";
import { userRoutes } from "./routes/users.js";
import { venueRoutes } from "./routes/venues.js";
import { verifyAccessToken } from "./utils/auth.js";

export async function buildApp() {
  const app = Fastify({
    logger: {
      level: env.nodeEnv === "development" ? "debug" : "info"
    }
  });

  await app.register(cors, {
    origin: env.corsOrigin.includes("*") ? true : env.corsOrigin
  });
  await app.register(multipart);
  await app.register(websocket);

  await app.register(healthRoutes);
  await app.register(authRoutes);
  await app.register(userRoutes);
  await app.register(venueRoutes);
  await app.register(discoveryRoutes);
  await app.register(matchRoutes);
  await app.register(safetyRoutes);

  app.get("/v1/ws", { websocket: true }, async (connection, request) => {
    const token = new URL(request.url, "http://localhost").searchParams.get("token");
    if (!token) {
      connection.close(1008, "Missing token");
      return;
    }

    try {
      verifyAccessToken(token);
    } catch {
      connection.close(1008, "Invalid token");
      return;
    }

    connection.send(JSON.stringify({ type: "connection.ready" }));
  });

  app.setErrorHandler((error, _request, reply) => {
    app.log.error(error);
    reply.code(500).send({ error: "Internal server error" });
  });

  return app;
}
