/**
 * JPA entities - persistent domain models.
 * <p>
 * {@link com.upisimulator.entity.BaseEntity} holds id + audit timestamps;
 * every entity here extends it. Phase 2 added {@link com.upisimulator.entity.User}
 * and {@link com.upisimulator.entity.RefreshToken}. Phase 4 adds
 * {@link com.upisimulator.entity.Wallet} and {@link com.upisimulator.entity.Transaction}
 * (a per-wallet ledger row). Still to come: {@code BankAccount}, {@code UpiId},
 * {@code MoneyRequest} (Phases 5, 6, 9).
 */
package com.upisimulator.entity;
