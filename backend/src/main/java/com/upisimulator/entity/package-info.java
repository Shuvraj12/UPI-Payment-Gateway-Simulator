/**
 * JPA entities - persistent domain models.
 * <p>
 * {@link com.upisimulator.entity.BaseEntity} holds id + audit timestamps;
 * every entity here extends it. Phase 2 adds {@link com.upisimulator.entity.User}
 * and {@link com.upisimulator.entity.RefreshToken}. Expands through Phases
 * 4-9 with {@code Wallet}, {@code BankAccount}, {@code UpiId},
 * {@code Transaction}, {@code MoneyRequest}.
 */
package com.upisimulator.entity;
