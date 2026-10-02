package com.apsfc.loanmanagement.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT settings bound from app.jwt.* (via .env / environment).
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, String issuer, long expirationMs) {

    /** HS256 requires a key of at least 32 bytes. */
    public String validateSecret() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("app.jwt.secret is not configured. Set JWT_SECRET (>= 32 chars).");
        }
        if (secret.getBytes().length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 characters.");
        }
        return secret;
    }
}