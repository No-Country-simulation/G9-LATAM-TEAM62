package com.g9latam.team62.fintech_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Email(regexp = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$", message = "El formato del correo es inválido")
        String email,
        @NotBlank(message = "La contraseña es obligatoria") String password) {
}
