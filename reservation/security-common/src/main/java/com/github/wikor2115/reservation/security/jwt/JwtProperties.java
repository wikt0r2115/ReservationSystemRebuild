package com.github.wikor2115.reservation.security.jwt;

import java.time.Duration;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "reservation.security.jwt")
public record JwtProperties(String secret, String issuer, Duration expiration) {
    public JwtProperties {
        Objects.requireNonNull(secret, "secret must not be null");
        Objects.requireNonNull(issuer, "issuer must not be null");
        Objects.requireNonNull(expiration, "expiration must not be null");
        if (secret.isBlank()) {
            throw new IllegalArgumentException("secret must not be blank");
        }
        if (issuer.isBlank()) {
            throw new IllegalArgumentException("issuer must not be blank");
        }
        if (expiration.isZero() || expiration.isNegative()) {
            throw new IllegalArgumentException("expiration must be positive");
        }
    }
}
