package com.g9latam.team62.fintech_api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "transactions", indexes = @Index(name = "idx_transactions_user_id", columnList = "user_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description;

    @Column(name = "operation_number", length = 100)
    private String operationNumber;

    // amount is always positive; direction is given by the category's type
    @NotNull
    @Positive
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Category category;

    // "date" is a reserved word in Oracle, hence the transaction_date column
    @NotNull
    @PastOrPresent
    @Column(name = "transaction_date", nullable = false)
    private LocalDate date;

    // eager because transactions are serialised to JSON outside the persistence
    // context, and a currency row is a single short string
    @NotNull
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "currency_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_transactions_currency"))
    private Currency currency;

    // account balance after this transaction was applied
    @Column(name = "balance_after", precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    // plain column instead of a @ManyToOne: the API exposes userId directly and
    // users are deleted through the service, which clears children first
    @NotNull
    @Column(name = "user_id", nullable = false)
    private Long userId;

    public TransactionType getType() {
        return category == null ? null : category.getType();
    }
}
