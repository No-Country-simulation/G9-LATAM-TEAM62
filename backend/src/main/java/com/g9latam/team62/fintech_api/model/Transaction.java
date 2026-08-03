package com.g9latam.team62.fintech_api.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    private Long id;
    private String description;
    private String operationNumber;

    // amount is always positive; direction is given by the category's type
    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private Category category;

    @NotNull
    @PastOrPresent
    private LocalDate date;

    @NotNull
    private Currency currency;

    // account balance after this transaction was applied
    private BigDecimal balanceAfter;

    @NotNull
    private Long userId;

    public TransactionType getType() {
        return category == null ? null : category.getType();
    }
}