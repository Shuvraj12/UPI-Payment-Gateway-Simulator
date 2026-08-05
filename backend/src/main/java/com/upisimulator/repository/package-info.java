/**
 * Spring Data JPA repositories.
 * <p>
 * Phase 2 adds {@code UserRepository} and {@code RefreshTokenRepository}.
 * From Phase 4 onward, every lookup here is scoped to the authenticated user
 * (e.g. {@code findByIdAndUserId}) rather than a bare {@code findById} - a
 * resource ID alone should never be enough to read someone else's wallet or
 * transaction.
 */
package com.upisimulator.repository;
