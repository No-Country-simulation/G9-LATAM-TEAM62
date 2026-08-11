package com.g9latam.team62.fintech_api.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String description;
    private String operationNumber;

    // amount is always positive; direction is given by the category's type
    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Category category;

    @NotNull
    @PastOrPresent
    private LocalDate date;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "currency_id")
    private Currency currency;

    // account balance after this transaction was applied
    private BigDecimal balanceAfter;

    @NotNull
    private Long userId;

    @Enumerated(EnumType.STRING)
    private TransactionSource source = TransactionSource.BANK;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    private LinkStatus linkStatus = LinkStatus.UNLINKED;

    private Long linkedTransactionId;

    @Enumerated(EnumType.STRING)
    private CategoryMethod categoryMethod;

    private Double categoryConfidence;

    private String bankName;

    public TransactionType getType() {
        return category == null ? null : category.getType();
    }
}
