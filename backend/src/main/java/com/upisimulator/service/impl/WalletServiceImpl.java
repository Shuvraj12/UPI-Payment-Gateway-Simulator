package com.upisimulator.service.impl;

import com.upisimulator.dto.DepositRequest;
import com.upisimulator.dto.TransactionResponse;
import com.upisimulator.dto.WalletResponse;
import com.upisimulator.entity.Transaction;
import com.upisimulator.entity.TransactionDirection;
import com.upisimulator.entity.TransactionStatus;
import com.upisimulator.entity.TransactionType;
import com.upisimulator.entity.User;
import com.upisimulator.entity.Wallet;
import com.upisimulator.exception.DuplicateResourceException;
import com.upisimulator.exception.InvalidRequestException;
import com.upisimulator.exception.ResourceNotFoundException;
import com.upisimulator.repository.TransactionRepository;
import com.upisimulator.repository.UserRepository;
import com.upisimulator.repository.WalletRepository;
import com.upisimulator.service.WalletService;
import com.upisimulator.util.UtrGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WalletServiceImpl implements WalletService {

    /**
     * A single simulated deposit is capped well below any real UPI P2P
     * limit - there's no bank account behind this yet (Phase 5), so it's a
     * deliberately modest, clearly-fake "top-up" mechanism to make Phase 4
     * demoable, not a real money-in pathway.
     */
    private static final BigDecimal MAX_SIMULATED_DEPOSIT = new BigDecimal("100000.00");

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Override
    public WalletResponse createWallet(Long userId) {
        if (walletRepository.existsByUserId(userId)) {
            throw new DuplicateResourceException("You already have a wallet");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setFrozen(false);

        Wallet saved = walletRepository.save(wallet);
        log.info("Wallet created for user id={}", userId);
        return toWalletResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public WalletResponse getWallet(Long userId) {
        return toWalletResponse(findWalletOrThrow(userId));
    }

    @Override
    public WalletResponse deposit(Long userId, DepositRequest request) {
        // PESSIMISTIC_WRITE: holds a row lock on this wallet until the
        // transaction commits, so a concurrent deposit on the same wallet
        // waits instead of racing on the read-modify-write below.
        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found. Create a wallet first."));

        if (wallet.isFrozen()) {
            throw new InvalidRequestException("Wallet is frozen. Unfreeze it before adding funds.");
        }
        if (request.amount().compareTo(MAX_SIMULATED_DEPOSIT) > 0) {
            throw new InvalidRequestException("Simulated deposits are capped at \u20B9" + MAX_SIMULATED_DEPOSIT);
        }

        BigDecimal newBalance = wallet.getBalance().add(request.amount()).setScale(2, RoundingMode.HALF_UP);
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        Transaction transaction = new Transaction();
        transaction.setWallet(wallet);
        transaction.setReferenceNumber(UtrGenerator.generate());
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setDirection(TransactionDirection.CREDIT);
        transaction.setAmount(request.amount());
        transaction.setBalanceAfter(newBalance);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setDescription(
                (request.description() == null || request.description().isBlank())
                        ? "Simulated deposit" : request.description());
        transactionRepository.save(transaction);

        log.info("Deposit of {} completed for user id={}, new balance={}", request.amount(), userId, newBalance);
        return toWalletResponse(wallet);
    }

    @Override
    public WalletResponse freeze(Long userId) {
        Wallet wallet = findWalletOrThrow(userId);
        if (wallet.isFrozen()) {
            throw new InvalidRequestException("Wallet is already frozen");
        }
        wallet.setFrozen(true);
        wallet.setFrozenAt(LocalDateTime.now());
        Wallet saved = walletRepository.save(wallet);
        log.info("Wallet frozen for user id={}", userId);
        return toWalletResponse(saved);
    }

    @Override
    public WalletResponse unfreeze(Long userId) {
        Wallet wallet = findWalletOrThrow(userId);
        if (!wallet.isFrozen()) {
            throw new InvalidRequestException("Wallet is not frozen");
        }
        wallet.setFrozen(false);
        wallet.setFrozenAt(null);
        Wallet saved = walletRepository.save(wallet);
        log.info("Wallet unfrozen for user id={}", userId);
        return toWalletResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactions(Long userId, Pageable pageable) {
        Wallet wallet = findWalletOrThrow(userId);
        return transactionRepository.findByWalletId(wallet.getId(), pageable)
                .map(this::toTransactionResponse);
    }

    private Wallet findWalletOrThrow(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found. Create a wallet first."));
    }

    private WalletResponse toWalletResponse(Wallet wallet) {
        return new WalletResponse(wallet.getId(), wallet.getBalance(), wallet.isFrozen(), wallet.getCreatedAt());
    }

    private TransactionResponse toTransactionResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getReferenceNumber(),
                transaction.getType(),
                transaction.getDirection(),
                transaction.getAmount(),
                transaction.getBalanceAfter(),
                transaction.getStatus(),
                transaction.getDescription(),
                transaction.getCounterpartyVpa(),
                transaction.getCreatedAt()
        );
    }

}
