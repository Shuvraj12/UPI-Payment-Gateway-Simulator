package com.upisimulator.service.impl;

import com.upisimulator.dto.AuthResponse;
import com.upisimulator.dto.DepositRequest;
import com.upisimulator.dto.RegisterRequest;
import com.upisimulator.dto.WalletResponse;
import com.upisimulator.service.AuthService;
import com.upisimulator.service.WalletService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Deliberately NOT {@code @Transactional} at the class level: that would
 * bind the whole test to one thread-local transaction/connection, which
 * defeats the point here - each worker thread below needs to open its own
 * real transaction against the real (H2) database for this to actually
 * exercise {@code WalletRepository.findByUserIdForUpdate}'s row lock rather
 * than just calling the service in-process with no real contention.
 * <p>
 * Without the lock, this test is flaky-to-reliably-failing (some deposits
 * get lost); with it, the ten deposits below always sum to exactly 1000.00.
 */
@SpringBootTest
class WalletConcurrencyIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private WalletService walletService;

    @Test
    void tenConcurrentDepositsAreAllAppliedWithNoLostUpdates() throws Exception {
        AuthResponse registered = authService.register(new RegisterRequest(
                "Concurrency Test", "concurrency-test@example.com", "SecurePass123", "9800000001"));
        Long userId = registered.user().id();
        walletService.createWallet(userId);

        int threadCount = 10;
        BigDecimal depositAmount = new BigDecimal("100.00");

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    walletService.deposit(userId, new DepositRequest(depositAmount, "concurrency test"));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Deposits did not all complete within the timeout");

        WalletResponse finalWallet = walletService.getWallet(userId);
        assertEquals(0, new BigDecimal("1000.00").compareTo(finalWallet.balance()),
                "Expected all 10 deposits of 100.00 to be reflected with no lost updates, got " + finalWallet.balance());
    }

}
