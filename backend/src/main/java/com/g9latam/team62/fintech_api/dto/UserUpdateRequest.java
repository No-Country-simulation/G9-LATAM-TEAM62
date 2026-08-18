package com.g9latam.team62.fintech_api.dto;

import com.g9latam.team62.fintech_api.model.SavingFrequency;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Cuerpo de {@code PUT /api/users/{id}}.
 *
 * <p>Reemplaza a la entidad {@code User}, que antes viajaba como cuerpo de la petición: por ahí
 * entraban {@code financialProfile}, {@code profileAccuracy} y {@code profileUpdatedAt}, campos
 * que solo escribe el análisis financiero. El servicio los preservaba a mano después de recibirlos;
 * ahora sencillamente no existen en la entrada.
 *
 * @param password contraseña nueva. Opcional: {@code null} deja la que ya tenía.
 */
public record UserUpdateRequest(
        @NotBlank(message = "El nombre es obligatorio")
        String name,

        @NotBlank(message = "El correo es obligatorio")
        @Email(regexp = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$", message = "El formato del correo es inválido")
        String email,

        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).*$", message = "La contraseña debe contener al menos una mayúscula y un número")
        String password,

        @PositiveOrZero(message = "El ingreso mensual debe ser mayor o igual a cero")
        BigDecimal monthlyIncome,

        SavingFrequency savingFrequency) {
}
