package com.upisimulator.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * One row per wallet-affecting event, from that wallet's own point of view.
 * A transfer (Phase 7) produces two rows sharing a reference number - a
 * DEBIT on the sender's wallet and a CREDIT on the receiver's - rather than
 * one row two wallets both point to, so each wallet's ledger is a simple,
 * self-contained list to query and page through.
 * <p>
 * Correction from Phase 4: {@code referenceNumber} was originally
 * {@code unique = true} on its own, which directly contradicted this
 * class's own "two rows share a reference number" design the moment a
 * second row actually needed to reuse one. Replaced with a composite
 * {@code (reference_number, direction)} constraint - exactly one DEBIT and
 * one CREDIT per UTR, which is the invariant that actually matters, while
 * still catching an accidental duplicate-direction bug.
 */
@Entity
@Table(
        name = "transactions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"reference_number", "direction"})
)
@Getter
@Setter
@NoArgsConstructor
public class Transaction extends BaseEntity {

    @Column(nullable = false, length = 12)
    private String referenceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionDirection direction;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /** Snapshot of the wallet's balance immediately after this entry - what makes a ledger a ledger. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column
    private String description;

    /**
     * Set only on the sender's (DEBIT) row of a transfer. Nullable columns
     * are excluded from MySQL/H2 unique-constraint comparisons (multiple
     * NULLs coexist fine), so DEPOSIT rows - which never carry a key - never
     * collide with each other or with real keys here.
     */
    @Column(unique = true)
    private String idempotencyKey;

    /** The other party's VPA - "who this was with" - populated for TRANSFER rows on both sides. */
    @Column
    private String counterpartyVpa;

}
