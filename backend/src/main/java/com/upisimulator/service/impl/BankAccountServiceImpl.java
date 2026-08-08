package com.upisimulator.service.impl;

import com.upisimulator.dto.AddBankAccountRequest;
import com.upisimulator.dto.BankAccountResponse;
import com.upisimulator.entity.BankAccount;
import com.upisimulator.entity.User;
import com.upisimulator.entity.VerificationStatus;
import com.upisimulator.exception.DuplicateResourceException;
import com.upisimulator.exception.InvalidRequestException;
import com.upisimulator.exception.ResourceNotFoundException;
import com.upisimulator.repository.BankAccountRepository;
import com.upisimulator.repository.UserRepository;
import com.upisimulator.service.BankAccountService;
import com.upisimulator.service.SimulatedOutcomeSource;
import com.upisimulator.util.MaskingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BankAccountServiceImpl implements BankAccountService {

    /** A real bank's "penny drop" check fails sometimes too - this isn't 100% on purpose. */
    private static final double VERIFICATION_SUCCESS_RATE = 0.85;

    private final BankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;
    private final SimulatedOutcomeSource outcomeSource;

    @Override
    public BankAccountResponse addBankAccount(Long userId, AddBankAccountRequest request) {
        if (bankAccountRepository.existsByUserIdAndBankNameAndAccountNumber(
                userId, request.bankName(), request.accountNumber())) {
            throw new DuplicateResourceException("This account is already linked to your profile");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean isFirstAccount = bankAccountRepository.countByUserId(userId) == 0;

        BankAccount account = new BankAccount();
        account.setUser(user);
        account.setAccountHolderName(request.accountHolderName());
        account.setBankName(request.bankName());
        account.setAccountNumber(request.accountNumber());
        account.setIfscCode(request.ifscCode());
        account.setAccountType(request.accountType());
        account.setPrimary(isFirstAccount);
        account.setVerificationStatus(VerificationStatus.PENDING);

        BankAccount saved = bankAccountRepository.save(account);
        log.info("Bank account added for user id={}, primary={}", userId, isFirstAccount);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankAccountResponse> getBankAccounts(Long userId) {
        return bankAccountRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public BankAccountResponse verifyBankAccount(Long userId, Long accountId) {
        BankAccount account = findOwnedAccountOrThrow(accountId, userId);

        if (account.getVerificationStatus() == VerificationStatus.VERIFIED) {
            throw new InvalidRequestException("This account is already verified");
        }

        boolean success = outcomeSource.succeeds(VERIFICATION_SUCCESS_RATE);
        if (success) {
            account.setVerificationStatus(VerificationStatus.VERIFIED);
            account.setVerifiedAt(LocalDateTime.now());
        } else {
            account.setVerificationStatus(VerificationStatus.FAILED);
        }

        BankAccount saved = bankAccountRepository.save(account);
        log.info("Verification {} for account id={} (user id={})",
                success ? "succeeded" : "failed", accountId, userId);
        return toResponse(saved);
    }

    @Override
    public BankAccountResponse setPrimary(Long userId, Long accountId) {
        BankAccount target = findOwnedAccountOrThrow(accountId, userId);

        if (target.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new InvalidRequestException("Only a verified account can be set as primary");
        }
        if (target.isPrimary()) {
            throw new InvalidRequestException("This account is already primary");
        }

        bankAccountRepository.findByUserIdAndPrimaryTrue(userId).ifPresent(current -> {
            current.setPrimary(false);
            bankAccountRepository.save(current);
        });

        target.setPrimary(true);
        BankAccount saved = bankAccountRepository.save(target);
        log.info("Account id={} set as primary for user id={}", accountId, userId);
        return toResponse(saved);
    }

    @Override
    public void deleteBankAccount(Long userId, Long accountId) {
        BankAccount account = findOwnedAccountOrThrow(accountId, userId);

        if (account.isPrimary() && bankAccountRepository.countByUserId(userId) > 1) {
            throw new InvalidRequestException("Set a different account as primary before deleting this one");
        }

        bankAccountRepository.delete(account);
        log.info("Bank account id={} deleted for user id={}", accountId, userId);
    }

    private BankAccount findOwnedAccountOrThrow(Long accountId, Long userId) {
        return bankAccountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Bank account not found"));
    }

    private BankAccountResponse toResponse(BankAccount account) {
        return new BankAccountResponse(
                account.getId(),
                account.getAccountHolderName(),
                account.getBankName(),
                MaskingUtil.maskAccountNumber(account.getAccountNumber()),
                account.getIfscCode(),
                account.getAccountType(),
                account.isPrimary(),
                account.getVerificationStatus(),
                account.getVerifiedAt(),
                account.getCreatedAt()
        );
    }

}
