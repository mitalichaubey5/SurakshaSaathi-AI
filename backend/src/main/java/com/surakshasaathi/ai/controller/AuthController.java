package com.surakshasaathi.ai.controller;

import com.surakshasaathi.ai.dto.ApiResponse;
import com.surakshasaathi.ai.dto.auth.AuthResponse;
import com.surakshasaathi.ai.dto.auth.LoginRequest;
import com.surakshasaathi.ai.dto.auth.MeResponse;
import com.surakshasaathi.ai.dto.auth.RegisterRequest;
import com.surakshasaathi.ai.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ApiResponse.ok("Registration successful", response);
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ApiResponse.ok("Login successful", response);
    }

    @GetMapping("/me")
    public ApiResponse<MeResponse> me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails userDetails)) {
            throw new IllegalArgumentException("Not authenticated");
        }
        MeResponse response = authService.getCurrentUser(userDetails.getUsername());
        return ApiResponse.ok("User profile", response);
    }
}
