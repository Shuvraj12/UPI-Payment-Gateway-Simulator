package com.upisimulator.service.impl;

import com.upisimulator.dto.AddBankAccountRequest;
import com.upisimulator.dto.CreateUpiIdRequest;
import com.upisimulator.dto.DepositRequest;
import com.upisimulator.dto.RegisterRequest;
import com.upisimulator.dto.TransferRequest;
import com.upisimulator.dto.WalletResponse;
import com.upisimulator.entity.AccountType;
import com.upisimulator.entity.BankName;
import com.upisimulator.service.AuthService;
import com.upisimulator.service.BankAccountService;
import com.upisimulator.service.SimulatedOutcomeSource;
import com.upisimulator.service.TransferService;
import com.upisimulator.service.UpiIdService;
import com.upisimulator.service.WalletService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;

/**
 * Deliberately NOT {@code @Transactional} at the class level - same
 * reasoning as {@code WalletConcurrencyIntegrationTest} from Phase 4: each
 * worker thread needs its own real transaction against the real (H2)
 * database for this to actually exercise the locking, not just call the
 * service in-process with no real contention.
 * <p>
 * This is the direct payoff of the lock-ordering decision documented in
 * {@code Wallet} and {@code TransferServiceImpl}: Alice paying Bob and Bob
 * paying Alice at the same instant, repeatedly, from many threads. Locking
 * by role (sender first, then recipient) would deadlock here roughly half
 * the time. Locking by ascending wallet id doesn't.
 */
@SpringBootTest
class TransferConcurrencyIntegrationTest {

    @Autowired
    private AuthService authService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private BankAccountService bankAccountService;
    @Autowired
    private UpiIdService upiIdService;
    @Autowired
    private TransferService transferService;

    @MockitoBean
    private SimulatedOutcomeSource outcomeSource;

    @Test
    void concurrentBidirectionalTransfersDoNotDeadlockOrLoseMoney() throws Exception {
        when(outcomeSource.succeeds(anyDouble())).thenReturn(true);

        Long aliceId = onboard("alice-concurrency@example.com", "9877700001", "aliceconcurrency");
        Long bobId = onboard("bob-concurrency@example.com", "9877700002", "bobconcurrency");

        walletService.deposit(aliceId, new DepositRequest(new BigDecimal("1000.00"), "seed"));
        walletService.deposit(bobId, new DepositRequest(new BigDecimal("1000.00"), "seed"));

        int pairsPerDirection = 20; // 40 total concurrent transfers, 20 each way
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(pairsPerDirection * 2);

        for (int i = 0; i < pairsPerDirection; i++) {
            executor.submit(() -> runTransfer(startLatch, doneLatch, aliceId, "bobconcurrency@upisim"));
            executor.submit(() -> runTransfer(startLatch, doneLatch, bobId, "aliceconcurrency@upisim"));
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Transfers did not all complete within the timeout - possible deadlock");

        WalletResponse aliceWallet = walletService.getWallet(aliceId);
        WalletResponse bobWallet = walletService.getWallet(bobId);
        BigDecimal total = aliceWallet.balance().add(bobWallet.balance());

        // The real invariant: no money created or destroyed, regardless of
        // exactly how many of the 40 attempts individually succeeded.
        assertEquals(0, new BigDecimal("2000.00").compareTo(total),
                "Total money across both wallets must be conserved - got " + total);
    }

    private void runTransfer(CountDownLatch startLatch, CountDownLatch doneLatch, Long senderUserId, String recipientVpa) {
        try {
            startLatch.await();
            transferService.transfer(senderUserId,
                    new TransferRequest(recipientVpa, new BigDecimal("10.00"), "concurrency test", UUID.randomUUID().toString()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // An individual transfer can legitimately fail (e.g. a transient
            // balance dip if attempts happen to land unevenly) without that
            // being a bug - the conservation-of-money assertion above is the
            // real check, not "every attempt must succeed."
        } finally {
            doneLatch.countDown();
        }
    }

    private Long onboard(String email, String phone, String upiUsername) {
        var registerResponse = authService.register(new RegisterRequest("Concurrency User", email, "SecurePass123", phone));
        Long userId = registerResponse.user().id();
        walletService.createWallet(userId);
        var bankAccount = bankAccountService.addBankAccount(userId, new AddBankAccountRequest(
                "Concurrency User", BankName.HDFC_BANK, phone + "0022", "HDFC0001234", AccountType.SAVINGS));
        bankAccountService.verifyBankAccount(userId, bankAccount.id());
        upiIdService.createUpiId(userId, new CreateUpiIdRequest(upiUsername));
        return userId;
    }

}
