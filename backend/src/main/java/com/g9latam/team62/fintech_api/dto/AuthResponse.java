package com.g9latam.team62.fintech_api.dto;

import com.g9latam.team62.fintech_api.model.User;

public record AuthResponse(
        String token,
        User user
) {}
