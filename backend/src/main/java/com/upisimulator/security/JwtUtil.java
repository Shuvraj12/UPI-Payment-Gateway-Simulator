package com.upisimulator.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Generic token generate/parse/validate utility, now wired into
 * {@link JwtAuthenticationFilter} and {@link com.upisimulator.service.impl.AuthServiceImpl}
 * as of Phase 2.
 * <p>
 * The access token is a plain stateless JWT. The refresh token additionally
 * carries a random {@code jti} claim, which {@code AuthServiceImpl} persists
 * in a {@link com.upisimulator.entity.RefreshToken} row - that's what makes
 * revocation (logout, rotation-on-refresh) possible without storing every
 * access token too.
 */
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

    /**
     * @param token the signed refresh token to hand back to the client
     * @param jti   the same token's JWT ID, for the caller to persist for revocation
     */
    public record TokenPair(String token, String jti) {
    }

    public String generateToken(String subject) {
        return buildToken(subject, jwtProperties.expiration(), null);
    }

    public TokenPair generateRefreshToken(String subject) {
        String jti = UUID.randomUUID().toString();
        String token = buildToken(subject, jwtProperties.refreshExpiration(), jti);
        return new TokenPair(token, jti);
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractJti(String token) {
        return extractAllClaims(token).getId();
    }

    public boolean isTokenValid(String token, String expectedUsername) {
        try {
            return extractUsername(token).equals(expectedUsername) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    private String buildToken(String subject, long expirationMillis, String jti) {
        Date issuedAt = new Date();
        Date expiry = new Date(issuedAt.getTime() + expirationMillis);
        var builder = Jwts.builder()
                .subject(subject)
                .issuedAt(issuedAt)
                .expiration(expiry);
        if (jti != null) {
            builder.id(jti);
        }
        return builder.signWith(getSigningKey()).compact();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.secret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

}
