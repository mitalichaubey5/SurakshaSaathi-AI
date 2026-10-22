package com.surakshasaathi.ai.dto.trusted;

import java.time.Instant;

public record TrustedContactResponse(
        Long id,
        String name,
        String phoneNumber,
        String email,
        String relationship,
        String preferredLanguage,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {}
