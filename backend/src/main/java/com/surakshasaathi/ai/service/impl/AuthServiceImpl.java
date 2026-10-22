package com.surakshasaathi.ai.service.impl;

import com.surakshasaathi.ai.dto.auth.AuthResponse;
import com.surakshasaathi.ai.dto.auth.LoginRequest;
import com.surakshasaathi.ai.dto.auth.MeResponse;
import com.surakshasaathi.ai.dto.auth.RegisterRequest;
import com.surakshasaathi.ai.model.Role;
import com.surakshasaathi.ai.model.User;
import com.surakshasaathi.ai.repository.UserRepository;
import com.surakshasaathi.ai.security.jwt.JwtService;
import com.surakshasaathi.ai.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }
        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        user.setEnabled(true);
        userRepository.save(user);

        // Auto-login after registration
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getEmail(), request.password()));
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(userDetails);
        return new AuthResponse(token, "Bearer", jwtService.getExpirationMs() / 1000,
                new AuthResponse.UserInfo(user.getId(), user.getName(), user.getEmail(), user.getRole().name()));
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().toLowerCase(), request.password()));
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(userDetails);
        User user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        return new AuthResponse(token, "Bearer", jwtService.getExpirationMs() / 1000,
                new AuthResponse.UserInfo(user.getId(), user.getName(), user.getEmail(), user.getRole().name()));
    }

    @Override
    public MeResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return new MeResponse(user.getId(), user.getName(), user.getEmail(), user.getRole().name());
    }
}
