# SurakshaSaathi AI - Trusted Circle (Step 7)

## Overview

This document describes the persistent, user-isolated Trusted Circle implementation. Trusted contacts are stored per authenticated user; ownership is enforced server-side and userId from the frontend is never trusted.

## Data Model

**TrustedContact** (`model/TrustedContact.java`)
- id (PK, auto-generated)
- user (ManyToOne to User, FK user_id, not exposed in responses)
- name (required, 2-100 chars)
- phoneNumber (optional, max 20)
- email (optional, valid format if provided)
- relationship (optional, max 50)
- preferredLanguage (default "en", max 10)
- enabled (default true)
- createdAt, updatedAt (auto-managed)

**Validation rule:** At least one of phoneNumber or email must be provided when creating a contact.

## API Endpoints

All `/api/v1/trusted-circle/contacts` endpoints require authentication (Bearer JWT). Contacts are returned only for the authenticated user.

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/trusted-circle/contacts` | Create trusted contact for current user |
| GET | `/api/v1/trusted-circle/contacts` | List all contacts for current user |
| GET | `/api/v1/trusted-circle/contacts/{id}` | Get one owned contact |
| PUT | `/api/v1/trusted-circle/contacts/{id}` | Update owned contact |
| DELETE | `/api/v1/trusted-circle/contacts/{id}` | Delete owned contact |

### Request/Response DTOs

- `CreateTrustedContactRequest`: name, phoneNumber, email, relationship, preferredLanguage
- `UpdateTrustedContactRequest`: name, phoneNumber, email, relationship, preferredLanguage, enabled
- `TrustedContactResponse`: id, name, phoneNumber, email, relationship, preferredLanguage, enabled, createdAt, updatedAt

### Existing Alert Endpoint

- `POST /api/v1/trusted-circle/alert` remains authenticated. By default it is still a mock notification (MOCK_SMS + MOCK_PUSH). An optional real WhatsApp path exists but is disabled unless both `WHATSAPP_ENABLED=true` and `WHATSAPP_SENDING_ENABLED=true` and credentials plus an allow-listed test recipient are configured (see `WHATSAPP_PHASE2.md`). No real SMS or email delivery is implemented.

**Important:** With the default configuration, Trusted Circle alert delivery is a mock/simulation and does not represent real SMS, WhatsApp, or email delivery. When real WhatsApp sending is explicitly enabled, a provider `ACCEPTED` response means the request was accepted - not that it was delivered.

## Security & Privacy

- Server-side ownership enforcement: all operations scope by current user's ID (via SecurityContext/JWT). User ID from frontend is never trusted.
- IDOR/BOLA protection: access to another user's contact returns 404 (safe, does not leak existence).
- Sensitive data: passwords/password hashes never logged; JWTs never logged; scam content/OCR text never stored; full contact details not logged unnecessarily.
- Validation: bean validation on all inputs; at least one contact method required.
- Stateless JWT auth, BCrypt password hashing, CORS restricted to configured origins.

## Database Configuration

Default: in-memory H2 for dev. To use MySQL 8:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/suraksha_saathi?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: ${DB_USER:root}
    password: ${DB_PASS:root}
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update  # For prototype; prefer migrations in production
    open-in-view: false
```

Environment variables: `DB_USER`, `DB_PASS`, `JWT_SECRET`, `OPENAI_API_KEY`, `TESSDATA_PREFIX` (for OCR). Credentials are never committed.

## Tests

Existing tests remain unchanged (33 backend + 19 frontend). Contact ownership and auth are enforced at service/repo level; integration tests for ownership scenarios can be added as needed. MySQL runtime was not tested in this environment.

## Build & Test

```bash
# Backend
cd backend
mvn test
mvn package

# Frontend
cd frontend
npm test
npm run build
```
