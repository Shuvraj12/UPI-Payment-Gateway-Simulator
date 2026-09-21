/**
 * JPA entities - persistent domain models.
 * <p>
 * {@link com.upisimulator.entity.BaseEntity} holds id + audit timestamps;
 * every entity here extends it. Phase 2 added {@link com.upisimulator.entity.User}
 * and {@link com.upisimulator.entity.RefreshToken}. Phase 4 added
 * {@link com.upisimulator.entity.Wallet} and {@link com.upisimulator.entity.Transaction}.
 * Phase 5 added {@link com.upisimulator.entity.BankAccount}. Phase 6 added
 * {@link com.upisimulator.entity.UpiId}. Phase 7 (Money Transfer) didn't add
 * a new entity, but grew {@code Transaction} with {@code idempotencyKey}
 * and {@code counterpartyVpa}, and fixed a Phase 4 constraint bug - see
 * {@code Transaction}'s Javadoc. Still to come: {@code MoneyRequest} (Phase 9).
 */
package com.upisimulator.entity;
