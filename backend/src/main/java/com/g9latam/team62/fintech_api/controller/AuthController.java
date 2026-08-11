package com.g9latam.team62.fintech_api.controller;

import com.g9latam.team62.fintech_api.dto.AuthResponse;
import com.g9latam.team62.fintech_api.dto.LoginRequest;
import com.g9latam.team62.fintech_api.model.User;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Endpoints para registro y autenticación de usuarios")
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
    @Operation(summary = "Iniciar sesión", description = "Autentica un usuario con sus credenciales y devuelve un token JWT junto con la información del usuario.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticación exitosa",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas o no autorizadas")
    })
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            User user = service.authenticate(request.email(), request.password());
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
            String token = jwtService.generateToken(userDetails);
            return ResponseEntity.ok(new AuthResponse(token, user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar nuevo usuario", description = "Crea un nuevo usuario en la plataforma.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario registrado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    })
    public ResponseEntity<User> register(@Valid @RequestBody User user) {
        User created = service.create(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
