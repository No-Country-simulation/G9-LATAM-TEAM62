package com.g9latam.team62.fintech_api.model;

public enum Category {
    FOOD(TransactionType.EXPENSE),
    TRANSPORT(TransactionType.EXPENSE),
    HOUSING(TransactionType.EXPENSE),
    UTILITIES(TransactionType.EXPENSE),
    ENTERTAINMENT(TransactionType.EXPENSE),
    HEALTH(TransactionType.EXPENSE),
    EDUCATION(TransactionType.EXPENSE),
    SHOPPING(TransactionType.EXPENSE),
    SALARY(TransactionType.INCOME),
    INVESTMENT(TransactionType.SAVING),
    SAVINGS(TransactionType.SAVING),
    OTHER_INCOME(TransactionType.INCOME),
    OTHER_EXPENSE(TransactionType.EXPENSE);

    private final TransactionType type;

    Category(TransactionType type) {
        this.type = type;
    }

    public TransactionType getType() {
        return type;
    }
}
