/**
 * Spring Data JPA repositories.
 * <p>
 * Phase 2 added {@code UserRepository} and {@code RefreshTokenRepository}.
 * Phase 4 adds {@code WalletRepository} (including a pessimistic-locking
 * lookup for balance mutations) and {@code TransactionRepository}. From
 * Phase 4 onward, every lookup here is scoped to the authenticated user
 * (e.g. {@code findByUserId}, {@code findByWalletId}) rather than a bare
 * {@code findById} - a resource ID alone should never be enough to read
 * someone else's wallet or transaction.
 */
package com.upisimulator.repository;
