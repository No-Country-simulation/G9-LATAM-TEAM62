package com.g9latam.team62.fintech_api.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    // account balance after this transaction was applied
    private BigDecimal balanceAfter;

    @NotNull
    private Long userId;

    public Transaction() {
    }

    public Transaction(Long id, String description, String operationNumber, BigDecimal amount, Category category,
                        LocalDate date, BigDecimal balanceAfter, Long userId) {
        this.id = id;
        this.description = description;
        this.operationNumber = operationNumber;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.balanceAfter = balanceAfter;
        this.userId = userId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOperationNumber() {
        return operationNumber;
    }

    public void setOperationNumber(String operationNumber) {
        this.operationNumber = operationNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public TransactionType getType() {
        return category == null ? null : category.getType();
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
