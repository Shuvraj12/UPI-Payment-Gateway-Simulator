package com.upisimulator.repository;

import com.upisimulator.entity.Transaction;
import com.upisimulator.entity.TransactionDirection;
import com.upisimulator.entity.TransactionStatus;
import com.upisimulator.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Deliberately just paginated, sort order coming entirely from the
     * caller's {@link Pageable} - no filter/search parameters here yet.
     * Phase 10 ("Transaction History": filters, search, export) is where
     * that grows; this stays the simple query it needs to be until then.
     */
    Page<Transaction> findByWalletId(Long walletId, Pageable pageable);

    /** Idempotent-replay lookup: has this exact client request already been processed? */
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    /**
     * Sum of a wallet's successful outgoing transfers since {@code startOfDay},
     * for the daily transfer limit check. Bound as actual enum parameters,
     * not string literals in the JPQL - comparing an {@code EnumType.STRING}
     * column against a bare string literal in JPQL isn't reliably portable
     * across Hibernate configurations, while a bound enum parameter always
     * goes through the same conversion the entity mapping itself uses.
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.wallet.id = :walletId AND t.type = :type AND t.direction = :direction " +
            "AND t.status = :status AND t.createdAt >= :startOfDay")
    BigDecimal sumAmountSince(@Param("walletId") Long walletId,
                              @Param("type") TransactionType type,
                              @Param("direction") TransactionDirection direction,
                              @Param("status") TransactionStatus status,
                              @Param("startOfDay") LocalDateTime startOfDay);

}
