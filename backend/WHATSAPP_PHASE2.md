# SurakshaSaathi AI - WhatsApp Cloud API Integration (Phase 2)

This document covers the optional, off-by-default real WhatsApp sending path and
the delivery-status tracking that was added in Phase 2. Phase 1 (webhook
verification + signature validation) is unchanged.

## Safety defaults

Real sending is **disabled by default**. The application only talks to Meta when
**all** of the following are true:

1. `app.whatsapp.enabled=true` (`WHATSAPP_ENABLED=true`) — enables the real
   `WhatsAppCloudClient` bean and outbound HTTP.
2. `app.whatsapp.sending-enabled=true` (`WHATSAPP_SENDING_ENABLED=true`) — a
   second, independent switch that gates *sending* only.
3. `WHATSAPP_ACCESS_TOKEN` and `WHATSAPP_PHONE_NUMBER_ID` are set.
4. At least one allow-listed recipient is configured
   (`WHATSAPP_TEST_RECIPIENT` and/or `WHATSAPP_TEST_RECIPIENTS`).

If anything is missing, the service silently falls back to the original mock
notification (`MOCK_SMS + MOCK_PUSH`) and **no** network call is made. The
webhook endpoint stays active regardless of these flags (Phase 1 behavior).

> Only Meta's **test phone number** may be used for sending. Never register or
> message a personal WhatsApp number.

## Environment variables

| Variable | Default | Purpose |
|---|---|---|
| `WHATSAPP_ENABLED` | `false` | Master switch for the real client. |
| `WHATSAPP_SENDING_ENABLED` | `false` | Independent sending switch. |
| `WHATSAPP_ACCESS_TOKEN` | _(empty)_ | Meta Cloud API token (never logged). |
| `WHATSAPP_PHONE_NUMBER_ID` | _(empty)_ | Test sender phone number id. |
| `WHATSAPP_APP_SECRET` | _(empty)_ | Webhook `X-Hub-Signature-256` verification. |
| `WHATSAPP_WEBHOOK_VERIFY_TOKEN` | _(empty)_ | GET webhook challenge token. |
| `WHATSAPP_TEST_RECIPIENT` | _(empty)_ | Single allow-listed recipient (E.164). |
| `WHATSAPP_TEST_RECIPIENTS` | _(empty)_ | Additional recipients, comma-separated. |
| `WHATSAPP_GRAPH_BASE_URL` | `https://graph.facebook.com` | Graph API base URL. |
| `WHATSAPP_API_VERSION` | `v21.0` | Pinned Graph version. |
| `WHATSAPP_TEMPLATE_NAME` | `hello_world` | Template to send. |
| `WHATSAPP_TEMPLATE_LANGUAGE` | `en_US` | Template language (must match). |
| `WHATSAPP_TIMEOUT_SECONDS` | `15` | Read timeout for a send. |

Secrets are supplied through the environment only and are never hardcoded,
logged, returned, or committed.

## Recipient allow-list policy

For a real send, the **request-supplied phone number is ignored entirely**.
Messages are only ever sent to the server-configured, allow-listed test
recipient. If no allow-listed recipient is configured, the send is rejected
(status `REJECTED_RECIPIENT`) and no message is sent.

## Delivery statuses

`DeliveryStatus` values, tracked forward-only:

```
MOCK, ACCEPTED, SENT, DELIVERED, READ, FAILED, UNKNOWN, REJECTED_RECIPIENT
```

- `ACCEPTED` — Meta accepted the request and returned a message id (wamid).
  **This is not delivery.**
- `SENT` / `DELIVERED` / `READ` — arrive asynchronously via the verified webhook.
- `FAILED` — provider rejected the send (numeric error code stored; raw body never stored).
- `UNKNOWN` — timeout or ambiguous network outcome. Never retried automatically.
- `REJECTED_RECIPIENT` — no allow-listed recipient; nothing sent.

Webhook updates are **forward-only** and idempotent: duplicate and out-of-order
events are ignored. `FAILED` may replace `ACCEPTED`/`SENT` but never
`DELIVERED`/`READ`; nothing follows a terminal state.

## Honesty contract

- A provider `ACCEPTED` response means the API accepted the request — **not**
  that the message was delivered.
- Only a valid, signature-verified Meta webhook advances a status to
  `DELIVERED`/`READ`.
- The `delivered` boolean in `TrustedAlertResponse` is only ever `true` for the
  mock path.

## API response

`POST /api/v1/trusted-circle/alert` is backward compatible. The original fields
are unchanged (`mockChannel` keeps its mock value). Two fields were appended:

- `status` — `MOCK`, or the real delivery status.
- `providerMessageId` — the Meta wamid when accepted, otherwise `null`.

## Database / MySQL migration (required before production)

Phase 2 extends the `AlertLog` entity with: `channel`, `provider_message_id`,
`delivery_status`, `provider_error_code`, and `status_updated_at`.

The prototype uses in-memory H2 with Hibernate `ddl-auto=update`, so no manual
step is needed for local runs. **A production MySQL deployment MUST apply a
schema migration adding these columns to `alert_logs` before deploying this
version.** Example:

```sql
ALTER TABLE alert_logs ADD COLUMN channel VARCHAR(20);
ALTER TABLE alert_logs ADD COLUMN provider_message_id VARCHAR(128);
ALTER TABLE alert_logs ADD COLUMN delivery_status VARCHAR(20);
ALTER TABLE alert_logs ADD COLUMN provider_error_code VARCHAR(20);
ALTER TABLE alert_logs ADD COLUMN status_updated_at DATETIME(6);
CREATE INDEX idx_alert_logs_provider_message_id ON alert_logs (provider_message_id);
```

## Security notes and limitations

- Signature verification, JWT protection, and the minimal public surface are
  unchanged from Phase 1.
- No secrets, message contents, raw provider bodies, or full recipient numbers
  are logged or returned.
- No automatic retries: an ambiguous outcome is recorded as `UNKNOWN`.
- Known limitations (pre-existing unless noted):
  - No rate/size limit on the public webhook POST.
  - No explicit replay window beyond HMAC signature validation.
  - Real Meta connectivity and the test number were **not** exercised in this
    environment (no credentials); all client behavior is covered by unit tests.

## Tests

Backend: `MetaWhatsAppCloudClientTest`, `DeliveryStatusTest`,
`WhatsAppDeliveryStatusServiceImplTest`, `TrustedCircleServiceImplTest`, and
`WhatsAppWebhookServiceImplTest` cover acceptance, provider errors, timeouts,
disabled sending, allow-list rejection, status transitions, duplicates, and
out-of-order events.

```bash
cd backend
mvn test
```
