/**
 * Business logic, as interfaces plus {@code impl} implementations.
 * <p>
 * Phase 2 added {@link com.upisimulator.service.AuthService}. Phase 3 added
 * {@link com.upisimulator.service.ProfileService} and
 * {@link com.upisimulator.service.FileStorageService}. Phase 4 added
 * {@link com.upisimulator.service.WalletService}. Phase 5 added
 * {@link com.upisimulator.service.BankAccountService} and
 * {@link com.upisimulator.service.SimulatedOutcomeSource}. Phase 6 adds
 * {@link com.upisimulator.service.UpiIdService}, which is the first service
 * to depend on three others' repositories at once (wallet + bank account
 * existence checks) to enforce that a UPI ID can't be created without a
 * funded, verified identity behind it. Controllers depend on the interface,
 * never the implementation, so services stay swappable and mockable in
 * tests.
 */
package com.upisimulator.service;
