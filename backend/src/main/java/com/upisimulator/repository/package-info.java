/**
 * Spring Data JPA repositories.
 * <p>
 * Phase 2 added {@code UserRepository} and {@code RefreshTokenRepository}.
 * Phase 4 added {@code WalletRepository} (including a pessimistic-locking
 * lookup for balance mutations) and {@code TransactionRepository}. Phase 5
 * added {@code BankAccountRepository}, the first repository where
 * ownership scoping actually matters, since {@code accountId} is a
 * client-supplied path variable. Phase 6 adds {@code UpiIdRepository},
 * which mostly follows that same rule (mutations go through
 * {@code findByIdAndUserId}) but deliberately breaks it for one method:
 * {@code findByVpa} is NOT user-scoped, because a VPA has to be resolvable
 * by whoever is paying it, not just its owner. Not every lookup should be
 * ownership-scoped - it depends on whether the resource is meant to be
 * privately owned (a bank account) or publicly resolvable (a UPI ID).
 */
package com.upisimulator.repository;
