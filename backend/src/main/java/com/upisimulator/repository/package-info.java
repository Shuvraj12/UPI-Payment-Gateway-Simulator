/**
 * Spring Data JPA repositories.
 * <p>
 * Empty in Phase 1. The first repository ({@code UserRepository}) arrives in
 * Phase 2, alongside the {@code User} entity. From Phase 4 onward, every
 * lookup here is scoped to the authenticated user (e.g. {@code findByIdAndUserId})
 * rather than a bare {@code findById} - a resource ID alone should never be
 * enough to read someone else's wallet or transaction.
 */
package com.upisimulator.repository;
