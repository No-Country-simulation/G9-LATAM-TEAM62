package com.g9latam.team62.fintech_api.dto;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public record StatementIngestionResult(
        @NotBlank (message = "Debe tener un estado")  
        String status,

        @NotBlank (message = "Debe tener un nombre de archivo")  
        String fileName,

        @NotBlank (message = "Debe tener un pais")  
        String country,

        @NotNull (message = "Debe tener un año")
        @Min(value = 1900, message = "El año debe ser mayor a 1900")
        Integer year,

        @NotNull (message = "El contador de valores duro no puede ser nulo")
        @PositiveOrZero(message = "El contador de valores duro no puede ser negativo")
        Integer rawRowsCount,

        @NotNull (message = "El contador de valores validos no puede ser nulo")
        @PositiveOrZero(message = "El contador de valores validos no puede ser negativo")
        Integer validRowsCount,

        @NotNull (message = "El contador de descartados no puede ser nulo")
        @PositiveOrZero(message = "El contador de descartados no puede ser negativo")
        Integer discardedRowsCount,
        
        List<String> warnings, 
        
        @NotEmpty(message = "Debe tener al menos una transacción")
        List<TransactionResponse> createdTransactions
) {
    public StatementIngestionResult {
        if (warnings == null) {
            warnings = new ArrayList<>();
        }
        if (createdTransactions == null) {
            createdTransactions = new ArrayList<>();
        }
    }
}