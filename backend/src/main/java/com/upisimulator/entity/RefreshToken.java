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

import java.time.LocalDateTime;

/**
 * One row per issued refresh token, identified by the JWT's own {@code jti}
 * claim rather than the full token string. This is what makes "logout"
 * actually mean something: without it, a stateless JWT refresh token stays
 * valid for its full 7-day life even after the user logs out.
 * <p>
 * Tokens are rotated on every use ({@link com.upisimulator.service.impl.AuthServiceImpl#refresh}
 * revokes the old row and issues a new one), so a stolen-but-unused refresh
 * token becomes worthless the moment the legitimate owner refreshes once.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String jti;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

}
