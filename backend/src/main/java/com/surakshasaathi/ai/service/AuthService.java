package com.surakshasaathi.ai.service;

import com.surakshasaathi.ai.dto.auth.AuthResponse;
import com.surakshasaathi.ai.dto.auth.LoginRequest;
import com.surakshasaathi.ai.dto.auth.RegisterRequest;
import com.surakshasaathi.ai.dto.auth.MeResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    MeResponse getCurrentUser(String email);
}
