/**
 * Entity &lt;-&gt; DTO conversion.
 * <p>
 * Still empty after Phase 2 - the one mapping it needed
 * ({@code User -> AuthResponse.UserSummary}, 3 fields) is written inline in
 * {@code AuthServiceImpl} instead. Introducing MapStruct's build-time
 * codegen for a single three-field mapping would be more machinery than the
 * problem calls for; revisiting this once Phase 4+ entities (Wallet,
 * Transaction, ...) create enough real mapping volume to justify it.
 */
package com.upisimulator.mapper;
