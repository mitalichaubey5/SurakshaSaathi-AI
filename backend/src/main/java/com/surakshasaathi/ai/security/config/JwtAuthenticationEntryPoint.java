package com.surakshasaathi.ai.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.surakshasaathi.ai.dto.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;

    /**
     * Uses the Spring Boot-managed ObjectMapper so date/time types (e.g. the
     * {@code Instant} timestamp on {@link ApiResponse}) serialise correctly.
     * A bare {@code new ObjectMapper()} lacks the JSR-310 module and throws on
     * the 401 body, breaking the JSON contract the frontend depends on.
     */
    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<Object> error = ApiResponse.error("Unauthorized", null);
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
