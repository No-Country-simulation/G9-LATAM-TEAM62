package com.g9latam.team62.fintech_api.controller;

import com.g9latam.team62.fintech_api.dto.AuthResponse;
import com.g9latam.team62.fintech_api.dto.LoginRequest;
import com.g9latam.team62.fintech_api.security.CustomUserDetailsService;
import com.g9latam.team62.fintech_api.security.JwtService;
import com.g9latam.team62.fintech_api.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService service;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public AuthController(UserService service, JwtService jwtService, CustomUserDetailsService userDetailsService) {
        this.service = service;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        return service.authenticate(request.email(), request.password())
                .<ResponseEntity<?>>map(user -> {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
                    String token = jwtService.generateToken(userDetails);
                    return ResponseEntity.ok(new AuthResponse(token, user));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "invalid email or password")));
    }
}
