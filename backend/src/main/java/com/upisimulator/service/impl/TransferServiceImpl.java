package com.upisimulator.service.impl;

import com.upisimulator.dto.ResolveRecipientResponse;
import com.upisimulator.dto.TransferRequest;
import com.upisimulator.dto.TransferResponse;
import com.upisimulator.entity.Transaction;
import com.upisimulator.entity.TransactionDirection;
import com.upisimulator.entity.TransactionStatus;
import com.upisimulator.entity.TransactionType;
import com.upisimulator.entity.UpiId;
import com.upisimulator.entity.Wallet;
import com.upisimulator.exception.InvalidRequestException;
import com.upisimulator.exception.ResourceNotFoundException;
import com.upisimulator.repository.TransactionRepository;
import com.upisimulator.repository.UpiIdRepository;
import com.upisimulator.repository.WalletRepository;
import com.upisimulator.service.TransferService;
import com.upisimulator.util.UtrGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

/**
 * The one part of this codebase where getting concurrency wrong loses or
 * duplicates money, so the design choices here get more explanation than
 * usual - see the inline comments at each step, and {@code Wallet}'s
 * Javadoc from Phase 4 for the locking strategy this builds on.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TransferServiceImpl implements TransferService {

    private static final BigDecimal MAX_TRANSFER_AMOUNT = new BigDecimal("100000.00");
    private static final BigDecimal DAILY_TRANSFER_LIMIT = new BigDecimal("200000.00");

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final UpiIdRepository upiIdRepository;

    @Override
    public TransferResponse transfer(Long senderUserId, TransferRequest request) {
        // Idempotency check first, before touching any wallet: if this exact
        // client request already completed, replay its result instead of
        // moving money again. A request that failed validation never gets
        // here on retry either, since only successful transfers persist a
        // row with this key - so retrying a genuinely-failed attempt still
        // processes normally.
        Optional<Transaction> replay = transactionRepository.findByIdempotencyKey(request.idempotencyKey());
        if (replay.isPresent()) {
            log.info("Idempotent replay for key={}, returning original result", request.idempotencyKey());
            return toTransferResponse(replay.get());
        }

        String recipientVpa = normalizeVpa(request.recipientVpa());
        UpiId recipientUpiId = upiIdRepository.findByVpa(recipientVpa)
                .orElseThrow(() -> new ResourceNotFoundException("Recipient UPI ID not found"));

        Wallet senderWalletUnlocked = walletRepository.findByUserId(senderUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found. Create a wallet first."));

        Long senderWalletId = senderWalletUnlocked.getId();
        Long recipientWalletId = recipientUpiId.getWallet().getId();

        if (senderWalletId.equals(recipientWalletId)) {
            throw new InvalidRequestException("You can't transfer to yourself");
        }

        UpiId senderDefaultUpiId = upiIdRepository.findByUserIdAndPrimaryTrue(senderUserId)
                .orElseThrow(() -> new InvalidRequestException("Create a UPI ID before sending money"));

        // Lock both wallets in a fixed order - ascending wallet id, regardless
        // of which one is sender vs recipient - not (sender, then recipient).
        // If Alice pays Bob and Bob pays Alice at the same instant, locking
        // by role would have transaction A hold Alice's lock waiting for
        // Bob's while transaction B holds Bob's lock waiting for Alice's:
        // deadlock. Locking by a role-independent, globally consistent order
        // means both transactions contend for the same wallet first, so one
        // just waits for the other instead.
        Long firstLockId = Math.min(senderWalletId, recipientWalletId);
        Long secondLockId = Math.max(senderWalletId, recipientWalletId);

        Wallet firstLocked = walletRepository.findByIdForUpdate(firstLockId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        Wallet secondLocked = walletRepository.findByIdForUpdate(secondLockId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        Wallet senderWallet = firstLocked.getId().equals(senderWalletId) ? firstLocked : secondLocked;
        Wallet recipientWallet = firstLocked.getId().equals(senderWalletId) ? secondLocked : firstLocked;

        // From here on, re-check everything against the LOCKED, current data -
        // the unlocked reads above were only ever good enough to determine
        // identities and lock order, not to validate against.
        if (senderWallet.isFrozen()) {
            throw new InvalidRequestException("Your wallet is frozen. Unfreeze it before sending money.");
        }
        if (recipientWallet.isFrozen()) {
            throw new InvalidRequestException("The recipient's wallet is frozen and can't receive funds right now.");
        }
        if (senderWallet.getBalance().compareTo(request.amount()) < 0) {
            throw new InvalidRequestException("Insufficient balance");
        }
        if (request.amount().compareTo(MAX_TRANSFER_AMOUNT) > 0) {
            throw new InvalidRequestException("Transfers are capped at \u20B9" + MAX_TRANSFER_AMOUNT + " per transaction");
        }

        BigDecimal alreadySentToday = transactionRepository.sumAmountSince(
                senderWallet.getId(), TransactionType.TRANSFER, TransactionDirection.DEBIT,
                TransactionStatus.SUCCESS, LocalDate.now().atStartOfDay());
        if (alreadySentToday.add(request.amount()).compareTo(DAILY_TRANSFER_LIMIT) > 0) {
            throw new InvalidRequestException("This would exceed your daily transfer limit of \u20B9" + DAILY_TRANSFER_LIMIT);
        }

        String utr = UtrGenerator.generate();

        BigDecimal senderNewBalance = senderWallet.getBalance().subtract(request.amount())
                .setScale(2, RoundingMode.HALF_UP);
        senderWallet.setBalance(senderNewBalance);
        walletRepository.save(senderWallet);

        BigDecimal recipientNewBalance = recipientWallet.getBalance().add(request.amount())
                .setScale(2, RoundingMode.HALF_UP);
        recipientWallet.setBalance(recipientNewBalance);
        walletRepository.save(recipientWallet);

        Transaction debitEntry = new Transaction();
        debitEntry.setWallet(senderWallet);
        debitEntry.setReferenceNumber(utr);
        debitEntry.setType(TransactionType.TRANSFER);
        debitEntry.setDirection(TransactionDirection.DEBIT);
        debitEntry.setAmount(request.amount());
        debitEntry.setBalanceAfter(senderNewBalance);
        debitEntry.setStatus(TransactionStatus.SUCCESS);
        debitEntry.setDescription(request.note());
        debitEntry.setCounterpartyVpa(recipientUpiId.getVpa());
        debitEntry.setIdempotencyKey(request.idempotencyKey());
        Transaction savedDebit = transactionRepository.save(debitEntry);

        Transaction creditEntry = new Transaction();
        creditEntry.setWallet(recipientWallet);
        creditEntry.setReferenceNumber(utr);
        creditEntry.setType(TransactionType.TRANSFER);
        creditEntry.setDirection(TransactionDirection.CREDIT);
        creditEntry.setAmount(request.amount());
        creditEntry.setBalanceAfter(recipientNewBalance);
        creditEntry.setStatus(TransactionStatus.SUCCESS);
        creditEntry.setDescription(request.note());
        creditEntry.setCounterpartyVpa(senderDefaultUpiId.getVpa());
        transactionRepository.save(creditEntry);

        log.info("Transfer {} completed: {} -> {} amount={}",
                utr, senderDefaultUpiId.getVpa(), recipientUpiId.getVpa(), request.amount());
        return toTransferResponse(savedDebit);
    }

    @Override
    @Transactional(readOnly = true)
    public ResolveRecipientResponse resolveRecipient(String vpa) {
        UpiId upiId = upiIdRepository.findByVpa(normalizeVpa(vpa))
                .orElseThrow(() -> new ResourceNotFoundException("Recipient UPI ID not found"));
        return new ResolveRecipientResponse(upiId.getVpa(), upiId.getUser().getFullName());
    }

    private String normalizeVpa(String vpa) {
        return vpa == null ? null : vpa.trim().toLowerCase();
    }

    private TransferResponse toTransferResponse(Transaction debitEntry) {
        return new TransferResponse(
                debitEntry.getReferenceNumber(),
                debitEntry.getCounterpartyVpa(),
                debitEntry.getAmount(),
                debitEntry.getBalanceAfter(),
                debitEntry.getStatus(),
                debitEntry.getCreatedAt()
        );
    }

}
