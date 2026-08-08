package com.upisimulator.service;

/**
 * Wraps the coin-flip behind any simulated pass/fail outcome (starting with
 * bank account verification). Kept as an injected dependency rather than a
 * direct {@code ThreadLocalRandom} call in the service so unit tests can
 * mock it and force a specific branch, and integration tests can override
 * the bean to make an otherwise-random flow deterministic end-to-end.
 */
public interface SimulatedOutcomeSource {

    /**
     * @param successRate probability of success, in [0.0, 1.0]
     */
    boolean succeeds(double successRate);

}
