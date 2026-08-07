package com.upisimulator.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One wallet per user (enforced by the unique {@code user_id} column).
 * <p>
 * Two different concurrency-control strategies are used deliberately, not
 * redundantly:
 * <ul>
 *   <li><b>Balance mutations</b> (deposit here; transfer/refund from
 *       Phase 7 onward) go through {@code WalletRepository.findByUserIdForUpdate},
 *       which takes a {@code SELECT ... FOR UPDATE} row lock for the whole
 *       transaction. Losing or double-applying a balance change is
 *       unacceptable, so a competing writer simply waits rather than racing.</li>
 *   <li><b>Everything else</b> (freeze/unfreeze) relies on the {@code @Version}
 *       column below - a much lower-contention path where "retry on
 *       conflict" is a perfectly fine failure mode, and holding a DB lock for
 *       it would be overkill.</li>
 * </ul>
 * Note for Phase 7: a transfer will need to lock two wallets (sender +
 * receiver) in the same transaction. Always acquire locks in a consistent
 * order (e.g. ascending wallet id) to avoid two concurrent transfers
 * deadlocking on each other.
 */
@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
public class Wallet extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean frozen = false;

    @Column
    private LocalDateTime frozenAt;

    @Version
    private Long version;

}
