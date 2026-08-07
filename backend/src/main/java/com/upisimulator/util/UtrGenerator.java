package com.upisimulator.util;

import java.util.concurrent.ThreadLocalRandom;

/**
 * A 12-digit numeric UTR, in the shape of a real UPI reference number.
 * Collision risk against the {@code transactions.reference_number} unique
 * constraint is negligible at this volume (1 in 900 billion per pair), so
 * no retry-on-collision loop is needed.
 */
public final class UtrGenerator {

    private static final long MIN = 100_000_000_000L;
    private static final long MAX_EXCLUSIVE = 1_000_000_000_000L;

    private UtrGenerator() {
    }

    public static String generate() {
        return String.valueOf(ThreadLocalRandom.current().nextLong(MIN, MAX_EXCLUSIVE));
    }

}
