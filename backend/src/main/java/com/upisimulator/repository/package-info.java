/**
 * Spring Data JPA repositories.
 * <p>
 * Phase 2 added {@code UserRepository} and {@code RefreshTokenRepository}.
 * Phase 4 added {@code WalletRepository} (including a pessimistic-locking
 * lookup for balance mutations) and {@code TransactionRepository}. Phase 5
 * adds {@code BankAccountRepository} - the first repository where ownership
 * scoping (Phase 4's package note) actually matters, since
 * {@code accountId} is a client-supplied path variable rather than always
 * being "the caller's own singular resource" the way Wallet and Profile
 * are. Every mutation there goes through {@code findByIdAndUserId}, never a
 * bare {@code findById}.
 */
package com.upisimulator.repository;
