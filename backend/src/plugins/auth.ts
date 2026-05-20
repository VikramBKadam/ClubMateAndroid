import { FastifyReply, FastifyRequest } from "fastify";
import { AuthContext, verifyAccessToken } from "../utils/auth.js";

declare module "fastify" {
  interface FastifyRequest {
    auth: AuthContext;
  }
}

export async function authenticate(
  request: FastifyRequest,
  reply: FastifyReply
): Promise<void> {
  const header = request.headers.authorization;
  const token = header?.startsWith("Bearer ") ? header.slice("Bearer ".length) : undefined;

  if (!token) {
    await reply.code(401).send({ error: "Missing bearer token" });
    return;
  }

  try {
    request.auth = verifyAccessToken(token);
  } catch {
    await reply.code(401).send({ error: "Invalid or expired token" });
  }
}
