package com.upisimulator.entity;

/**
 * A newly added account starts {@code PENDING}. {@code VERIFIED}/{@code FAILED}
 * are reached only through the explicit verify action (Phase 5's simulation
 * of a real bank's "penny drop" check) - never automatically on add.
 */
public enum VerificationStatus {
    PENDING,
    VERIFIED,
    FAILED
}
