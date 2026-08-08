package com.upisimulator.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A bank account a user has linked to their profile. Deliberately NOT wired
 * into {@link Wallet} balance mechanics - this simulator's money movement
 * stays wallet-to-wallet via UPI ID (Phase 6/7), same as most UPI apps'
 * actual transfer path. Bank accounts exist here as the linked-account
 * records every real UPI app shows, and as a foundation a "top up from
 * bank" feature could build on later without needing a schema change.
 * <p>
 * Hard-deleted (unlike {@link User}'s soft delete) because nothing else
 * references a bank account yet - soft delete is worth its complexity only
 * where another table's foreign key would otherwise dangle or cascade.
 */
@Entity
@Table(name = "bank_accounts")
@Getter
@Setter
@NoArgsConstructor
public class BankAccount extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String accountHolderName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BankName bankName;

    /** Stored in full internally; every response masks all but the last 4 digits (see MaskingUtil). */
    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false, length = 11)
    private String ifscCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType accountType;

    @Column(nullable = false)
    private boolean primary = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column
    private LocalDateTime verifiedAt;

}
