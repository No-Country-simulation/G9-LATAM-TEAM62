package com.g9latam.team62.fintech_api.dto;

import com.g9latam.team62.fintech_api.model.Category;
import com.g9latam.team62.fintech_api.model.PaymentMethod;
import com.g9latam.team62.fintech_api.model.Transaction;
import com.g9latam.team62.fintech_api.model.TransactionSource;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Cuerpo de {@code POST /api/transactions} y {@code PUT /api/transactions/{id}}.
 *
 * <p>Antes estos endpoints recibían la entidad {@link Transaction} directamente, lo que dejaba en
 * manos del cliente campos que decide el servidor: {@code categoryMethod}, {@code
 * categoryConfidence}, {@code linkStatus}, {@code linkedTransactionId} y el propio {@code id}.
 * Nada impedía declarar una clasificación inventada como si la hubiera hecho el motor. Aquí solo
 * viajan los datos del movimiento; el resto lo asigna {@code TransactionService}.
 *
 * <p>La categoría es opcional a propósito: omitirla es la forma de pedir clasificación automática.
 */
public record TransactionRequest(
        @NotNull(message = "El ID de usuario es obligatorio")
        Long userId,

        @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
        String description,

        @Size(max = 100, message = "El número de operación no puede superar los 100 caracteres")
        String operationNumber,

        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser un valor positivo mayor a cero")
        BigDecimal amount,

        Category category,

        @NotNull(message = "La fecha es obligatoria")
        @PastOrPresent(message = "La fecha no puede estar en el futuro")
        LocalDate date,

        CurrencyRef currency,

        BigDecimal balanceAfter,

        TransactionSource source,

        PaymentMethod paymentMethod,

        @Size(max = 100, message = "El nombre del banco no puede superar los 100 caracteres")
        String bankName
) {

    /** Entidad con los campos que el cliente sí decide; los demás quedan para el servicio. */
    public Transaction toEntity() {
        Transaction transaction = new Transaction();
        transaction.setUserId(userId);
        transaction.setDescription(description);
        transaction.setOperationNumber(operationNumber);
        transaction.setAmount(amount);
        transaction.setCategory(category);
        transaction.setDate(date);
        transaction.setCurrency((currency != null ? currency : CurrencyRef.DEFAULT).toReference());
        transaction.setBalanceAfter(balanceAfter);
        transaction.setSource(source);
        transaction.setPaymentMethod(paymentMethod);
        transaction.setBankName(bankName);
        return transaction;
    }
}
