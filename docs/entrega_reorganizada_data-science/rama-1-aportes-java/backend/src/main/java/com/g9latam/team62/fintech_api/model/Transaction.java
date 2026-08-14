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
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

// RECONCILIADO contra la versión real del repo (la que migró a JPA con Currency).
// Todo lo marcado "-- aporte --" es nuevo; el resto es exactamente el archivo
// real, sin tocar su forma ni sus anotaciones existentes.
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

    // -- aporte -- ya no es @NotNull: una transacción BANK puede llegar sin
    // categoría y el futuro CategoryClassifierService la completa antes de
    // guardar (ver TransactionService.create()). Las transacciones MANUAL
    // siempre traen categoría desde el DTO, que sí la exige.
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
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

    // ------------------------------------------------------------------
    // -- aporte -- origen (banco/manual), medio de pago, conciliación con
    // la cartola bancaria, y trazabilidad de cómo se determinó la categoría.
    // Ver db/oracle/002_manual_entries_and_budget.sql para el ALTER TABLE.
    // ------------------------------------------------------------------

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionSource source = TransactionSource.BANK;

    // solo aplica a MANUAL (tarjeta de crédito excluida del MVP a propósito)
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 10)
    private PaymentMethod paymentMethod;

    // solo relevante para MANUAL: si coincide con una transacción BANK real
    // (mismo monto, ventana de 2-3 días), queda AUTOMATIC o USER_CONFIRMED.
    // EFECTIVO nunca se intenta conciliar -- nunca aparecerá en un banco.
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "link_status", nullable = false, length = 20)
    private LinkStatus linkStatus = LinkStatus.UNLINKED;

    @Column(name = "linked_transaction_id")
    private Long linkedTransactionId;

    // cómo se determinó `category`: mapeo, regla, modelo, fallback, o el
    // usuario (directamente o corrigiendo una sugerencia)
    @Enumerated(EnumType.STRING)
    @Column(name = "category_method", length = 20)
    private CategoryMethod categoryMethod;

    // probabilidad del modelo cuando categoryMethod == ML_MODEL; null en
    // cualquier otro caso (no hay una probabilidad real que reportar)
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    @Column(name = "category_confidence")
    private Double categoryConfidence;

    public TransactionType getType() {
        return category == null ? null : category.getType();
    }
}
