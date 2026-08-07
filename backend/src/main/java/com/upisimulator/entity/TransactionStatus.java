package com.upisimulator.entity;

/**
 * Phase 4's deposit is synchronous - it either completes in one transaction
 * or throws before any row is written, so only {@code SUCCESS} is reachable
 * today. {@code PENDING} and {@code FAILED} become meaningful once transfers
 * (Phase 7) can fail partway (e.g. a frozen recipient wallet) - see "Handle
 * failed transactions" in the project brief.
 */
public enum TransactionStatus {
    SUCCESS,
    PENDING,
    FAILED
}
