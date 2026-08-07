package com.upisimulator.entity;

/**
 * Only {@code DEPOSIT} is actually creatable in Phase 4 (no bank accounts or
 * transfers exist yet). The rest are reserved so this enum doesn't need a
 * disruptive change when those phases land.
 */
public enum TransactionType {
    DEPOSIT,
    TRANSFER,
    QR_PAYMENT,
    REQUEST_SETTLEMENT,
    REFUND
}
