package com.upisimulator.repository;

import com.upisimulator.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Deliberately just paginated, sort order coming entirely from the
     * caller's {@link Pageable} - no filter/search parameters here yet.
     * Phase 10 ("Transaction History": filters, search, export) is where
     * that grows; this stays the simple query it needs to be until then.
     */
    Page<Transaction> findByWalletId(Long walletId, Pageable pageable);

}
