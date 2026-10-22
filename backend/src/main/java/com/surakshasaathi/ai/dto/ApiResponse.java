package com.surakshasaathi.ai.dto;

import java.time.Instant;

/**
 * Uniform envelope for every API response so the frontend always sees one shape:
 * { "success": true|false, "message": "...", "data": {...}, "timestamp": "..." }
 */
public record ApiResponse<T>(boolean success, String message, T data, Instant timestamp) {

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, Instant.now());
    }

    public static <T> ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>(false, message, data, Instant.now());
    }
}
