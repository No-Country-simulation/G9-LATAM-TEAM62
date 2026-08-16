package com.g9latam.team62.fintech_api.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank(message = "La contraseña anterior es obligatoria") String oldPassword,
        @NotBlank(message = "La nueva contraseña es obligatoria") String newPassword
) {}
