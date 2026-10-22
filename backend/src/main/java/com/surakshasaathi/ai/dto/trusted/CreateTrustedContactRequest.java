package com.surakshasaathi.ai.dto.trusted;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTrustedContactRequest(
        @NotBlank @Size(min = 2, max = 100) String name,
        @Size(max = 20) String phoneNumber,
        @Email @Size(max = 100) String email,
        @Size(max = 50) String relationship,
        @Size(max = 10) String preferredLanguage
) {}
