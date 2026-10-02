package com.apsfc.loanmanagement.dto.response;

/**
 * Successful authentication payload: the signed JWT plus the caller's profile.
 * Client stores the token and sends it as: Authorization: Bearer <token>.
 */
public record AuthResponse(
        String token,
        String tokenType,
        long expiresInMs,
        UserResponse user
) {
}