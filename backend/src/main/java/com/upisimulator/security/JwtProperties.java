package com.upisimulator.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the {@code jwt.*} keys from application.yml (secret, access-token
 * expiry, refresh-token expiry). A record is enough here - this is plain
 * immutable configuration data, not a bean with behaviour - and
 * {@code @ConfigurationPropertiesScan} on the main class registers it
 * without needing {@code @Component} or getters/setters.
 * <p>
 * {@code refreshExpiration} binds to {@code jwt.refresh-expiration} in YAML
 * via Spring's relaxed binding (camelCase <-> kebab-case).
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secret, long expiration, long refreshExpiration) {
}
