package com.g9latam.team62.fintech_api.dto;

import com.g9latam.team62.fintech_api.model.Category;
import com.g9latam.team62.fintech_api.model.Currency;
import com.g9latam.team62.fintech_api.model.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ManualTransactionRequest(
        @NotNull Long userId,
        @NotNull @Positive BigDecimal amount,
        @NotNull Category category,
        String description,
        Currency currency,
        PaymentMethod paymentMethod,
        String bankName,
        String operationNumber
) {}
