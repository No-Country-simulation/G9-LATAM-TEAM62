package com.g9latam.team62.fintech_api.dto;

import com.g9latam.team62.fintech_api.model.SavingFrequency;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RegisterRequest(
        @NotBlank(message = "El nombre es obligatorio")
        String name,

        @NotBlank(message = "El correo es obligatorio")
        @Email(regexp = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$", message = "El formato del correo es inválido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).*$", message = "La contraseña debe contener al menos una mayúscula y un número")
        String password,

        @PositiveOrZero(message = "El ingreso mensual debe ser mayor o igual a cero")
        BigDecimal monthlyIncome,

        SavingFrequency savingFrequency
) {
    public RegisterRequest(String name, String email, String password) {
        this(name, email, password, null, null);
    }
}