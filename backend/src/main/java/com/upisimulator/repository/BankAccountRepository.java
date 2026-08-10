package com.upisimulator.repository;

import com.upisimulator.entity.BankAccount;
import com.upisimulator.entity.BankName;
import com.upisimulator.entity.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    List<BankAccount> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Every mutation (verify, set-primary, delete) goes through this, not a
     * bare {@code findById} - {@code accountId} is client-supplied in the
     * URL, so ownership has to be part of the query, not a check bolted on
     * after the fact. Returning empty here (and thus a 404, not a 403) also
     * means a caller can't tell "doesn't exist" from "exists but isn't
     * yours" by probing IDs.
     */
    Optional<BankAccount> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndBankNameAndAccountNumber(Long userId, BankName bankName, String accountNumber);

    boolean existsByUserIdAndVerificationStatus(Long userId, VerificationStatus verificationStatus);

    Optional<BankAccount> findByUserIdAndPrimaryTrue(Long userId);

    long countByUserId(Long userId);

}
