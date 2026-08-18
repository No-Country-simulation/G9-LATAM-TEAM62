package com.g9latam.team62.fintech_api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.g9latam.team62.fintech_api.model.Category;
import com.g9latam.team62.fintech_api.model.CategoryMethod;
import com.g9latam.team62.fintech_api.model.Currency;
import com.g9latam.team62.fintech_api.model.LinkStatus;
import com.g9latam.team62.fintech_api.model.PaymentMethod;
import com.g9latam.team62.fintech_api.model.Transaction;
import com.g9latam.team62.fintech_api.model.TransactionSource;
import com.g9latam.team62.fintech_api.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Cómo se ve una transacción para quien consume la API. Mantiene los mismos nombres que exponía la
 * entidad —el frontend y el API Tester ya los usan— pero desacopla la respuesta de la tabla: una
 * columna nueva deja de asomarse sola en el JSON.
 */
public record TransactionResponse(
        Long id,
        String description,
        String operationNumber,
        BigDecimal amount,
        Category category,
        LocalDate date,
        CurrencyResponse currency,
        BigDecimal balanceAfter,
        Long userId,
        TransactionSource source,
        PaymentMethod paymentMethod,
        LinkStatus linkStatus,
        Long linkedTransactionId,
        CategoryMethod categoryMethod,
        Double categoryConfidence,
        String bankName,
        TransactionType type
) {

    public record CurrencyResponse(Long id, @JsonProperty("name_currency") String nameCurrency) {
        static CurrencyResponse fromEntity(Currency currency) {
            return currency == null ? null : new CurrencyResponse(currency.getId(), currency.getNameCurrency());
        }
    }

    public static TransactionResponse fromEntity(Transaction transaction) {
        if (transaction == null) {
            return null;
        }
        return new TransactionResponse(
                transaction.getId(),
                transaction.getDescription(),
                transaction.getOperationNumber(),
                transaction.getAmount(),
                transaction.getCategory(),
                transaction.getDate(),
                CurrencyResponse.fromEntity(transaction.getCurrency()),
                transaction.getBalanceAfter(),
                transaction.getUserId(),
                transaction.getSource(),
                transaction.getPaymentMethod(),
                transaction.getLinkStatus(),
                transaction.getLinkedTransactionId(),
                transaction.getCategoryMethod(),
                transaction.getCategoryConfidence(),
                transaction.getBankName(),
                transaction.getType());
    }

    public static List<TransactionResponse> fromEntities(List<Transaction> transactions) {
        return transactions.stream().map(TransactionResponse::fromEntity).toList();
    }
}
