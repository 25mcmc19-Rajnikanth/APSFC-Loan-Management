package com.apsfc.loanmanagement.dto.response;

import com.apsfc.loanmanagement.enums.UserStatus;

/**
 * Safe, minimal representation of a user. Never includes the password hash.
 */
public record UserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        String phone,
        String role,
        UserStatus status
) {
}