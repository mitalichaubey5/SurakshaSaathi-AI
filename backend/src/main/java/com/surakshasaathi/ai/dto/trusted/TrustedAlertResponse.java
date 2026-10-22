package com.surakshasaathi.ai.dto.trusted;

import java.time.Instant;

/**
 * Response for POST /api/v1/trusted-circle/alert.
 *
 * <p>Phase 2 is additive and backward compatible: the original fields are
 * unchanged ({@code mockChannel} is retained with its mock value). Two new
 * fields are appended:</p>
 * <ul>
 *   <li>{@code status} - {@code MOCK} in mock mode, otherwise the delivery status
 *       (e.g. {@code ACCEPTED}/{@code UNKNOWN}/{@code FAILED}/{@code REJECTED_RECIPIENT}).
 *       An {@code ACCEPTED} status means the provider accepted the request, not
 *       that it was delivered.</li>
 *   <li>{@code providerMessageId} - the Meta message id (wamid) when the provider
 *       accepted the message, otherwise {@code null}.</li>
 * </ul>
 */
public record TrustedAlertResponse(
        boolean delivered,
        String confirmationMessage,
        String mockChannel,
        String alertId,
        Instant timestamp,
        String status,
        String providerMessageId
) {
}
