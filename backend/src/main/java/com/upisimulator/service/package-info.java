/**
 * Business logic, as interfaces plus {@code impl} implementations.
 * <p>
 * Phase 2 added {@link com.upisimulator.service.AuthService}. Phase 3 added
 * {@link com.upisimulator.service.ProfileService} and
 * {@link com.upisimulator.service.FileStorageService}. Phase 4 adds
 * {@link com.upisimulator.service.WalletService}. Controllers depend on the
 * interface, never the implementation, so services stay swappable and
 * mockable in tests.
 */
package com.upisimulator.service;
