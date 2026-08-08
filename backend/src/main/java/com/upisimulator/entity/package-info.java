/**
 * JPA entities - persistent domain models.
 * <p>
 * {@link com.upisimulator.entity.BaseEntity} holds id + audit timestamps;
 * every entity here extends it. Phase 2 added {@link com.upisimulator.entity.User}
 * and {@link com.upisimulator.entity.RefreshToken}. Phase 4 added
 * {@link com.upisimulator.entity.Wallet} and {@link com.upisimulator.entity.Transaction}.
 * Phase 5 adds {@link com.upisimulator.entity.BankAccount} (linked-account
 * records, intentionally not wired into wallet balance mechanics - see its
 * Javadoc). Still to come: {@code UpiId}, {@code MoneyRequest} (Phases 6, 9).
 */
package com.upisimulator.entity;
