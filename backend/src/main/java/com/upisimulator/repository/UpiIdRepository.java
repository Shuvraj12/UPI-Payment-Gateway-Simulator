package com.upisimulator.repository;

import com.upisimulator.entity.UpiId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UpiIdRepository extends JpaRepository<UpiId, Long> {

    List<UpiId> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** Mutations (set-default) go through this - same ownership-scoping reasoning as BankAccountRepository. */
    Optional<UpiId> findByIdAndUserId(Long id, Long userId);

    boolean existsByVpa(String vpa);

    Optional<UpiId> findByUserIdAndPrimaryTrue(Long userId);

    long countByUserId(Long userId);

    /**
     * Deliberately NOT scoped to a user - a VPA has to be resolvable by
     * whoever is sending money to it, not just its owner. Not called from
     * any controller yet; this is here for Phase 7's transfer flow to
     * resolve "pay priya@upisim" to a wallet.
     */
    Optional<UpiId> findByVpa(String vpa);

}
