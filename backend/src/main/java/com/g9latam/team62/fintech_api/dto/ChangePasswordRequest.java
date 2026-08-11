package com.g9latam.team62.fintech_api.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank String oldPassword,
        @NotBlank String newPassword
) {}
