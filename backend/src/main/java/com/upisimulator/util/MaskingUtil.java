package com.upisimulator.util;

public final class MaskingUtil {

    private static final int VISIBLE_DIGITS = 4;

    private MaskingUtil() {
    }

    /**
     * Replaces every character except the last 4 with 'X', e.g.
     * {@code "1234567890123"} becomes {@code "XXXXXXXXX0123"}. Short input
     * (nothing meaningful to hide) is returned as-is.
     */
    public static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= VISIBLE_DIGITS) {
            return accountNumber;
        }
        String masked = "X".repeat(accountNumber.length() - VISIBLE_DIGITS);
        return masked + accountNumber.substring(accountNumber.length() - VISIBLE_DIGITS);
    }

}
