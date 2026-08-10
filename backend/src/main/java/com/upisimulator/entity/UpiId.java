package com.upisimulator.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A UPI ID (VPA) resolves to a {@link Wallet}, not a {@link BankAccount} -
 * this simulator's actual balance holder is the wallet (Phase 4), so that's
 * what a transfer (Phase 7) needs to reach as directly as possible. Both
 * {@code user} and {@code wallet} are stored: {@code user} for "whose UPI
 * IDs are these" (listing), {@code wallet} as the fast lookup path a
 * transfer will hit on every send.
 * <p>
 * The boolean flag is named {@code primary}, not {@code default} - {@code default}
 * alone is a reserved word in Java and can't be a field name. The public
 * API still uses "default" terminology (matching the phase brief) via
 * {@code UpiIdResponse.isDefault}; only the internal field name differs.
 */
@Entity
@Table(name = "upi_ids")
@Getter
@Setter
@NoArgsConstructor
public class UpiId extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(nullable = false, unique = true)
    private String vpa;

    @Column(nullable = false)
    private boolean primary = false;

}
