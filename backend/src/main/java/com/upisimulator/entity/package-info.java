/**
 * JPA entities - persistent domain models.
 * <p>
 * {@link com.upisimulator.entity.BaseEntity} holds id + audit timestamps;
 * every entity here extends it. Phase 2 added {@link com.upisimulator.entity.User}
 * and {@link com.upisimulator.entity.RefreshToken}. Phase 4 added
 * {@link com.upisimulator.entity.Wallet} and {@link com.upisimulator.entity.Transaction}.
 * Phase 5 added {@link com.upisimulator.entity.BankAccount}. Phase 6 adds
 * {@link com.upisimulator.entity.UpiId}, which resolves to a
 * {@code Wallet} - see its Javadoc for why, not a {@code BankAccount}.
 * Still to come: {@code MoneyRequest} (Phase 9).
 */
package com.upisimulator.entity;
