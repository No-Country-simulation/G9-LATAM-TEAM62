package com.g9latam.team62.fintech_api.dto;

import com.g9latam.team62.fintech_api.model.Transaction;

import java.util.List;

public record StatementIngestionResult(
        String status,
        String fileName,
        String country,
        Integer year,
        Integer rawRowsCount,
        Integer validRowsCount,
        Integer discardedRowsCount,
        List<String> warnings,
        List<Transaction> createdTransactions
) {}
