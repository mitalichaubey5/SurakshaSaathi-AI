package com.surakshasaathi.ai.dto.auth;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserInfo user
) {
    public static record UserInfo(Long id, String name, String email, String role) {}
}
