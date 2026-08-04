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

/**
 * Generic token generate/parse/validate utility. Deliberately NOT wired into
 * a request filter yet, and not tied to a {@code UserDetailsService} - there's
 * no {@code User} entity until Phase 2. This class is the reusable piece
 * Phase 2's actual login/register/refresh endpoints will call into.
 */
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

    public String generateToken(String subject) {
        return buildToken(subject, jwtProperties.expiration());
    }

    public String generateRefreshToken(String subject) {
        return buildToken(subject, jwtProperties.refreshExpiration());
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
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

    private String buildToken(String subject, long expirationMillis) {
        Date issuedAt = new Date();
        Date expiry = new Date(issuedAt.getTime() + expirationMillis);
        return Jwts.builder()
                .subject(subject)
                .issuedAt(issuedAt)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
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
