import { getJson, postJson } from './http';

/**
 * Authentication calls for the Spring Boot backend:
 *
 *   POST /api/v1/auth/register  { name, email, password } -> AuthResponse
 *   POST /api/v1/auth/login     { email, password }       -> AuthResponse
 *   GET  /api/v1/auth/me                                  -> MeResponse
 *
 * AuthResponse: { accessToken, tokenType, expiresIn, user: { id, name, email, role } }
 * MeResponse:   { id, name, email, role }
 */

export function register({ name, email, password }) {
  return postJson('/api/v1/auth/register', { name, email, password });
}

export function login({ email, password }) {
  return postJson('/api/v1/auth/login', { email, password });
}

export function me() {
  return getJson('/api/v1/auth/me');
}
