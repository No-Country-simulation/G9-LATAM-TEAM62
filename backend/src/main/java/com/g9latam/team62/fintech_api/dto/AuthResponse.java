package com.g9latam.team62.fintech_api.dto;

public record AuthResponse(
        String token,
        UserResponseDTO user
) {}

