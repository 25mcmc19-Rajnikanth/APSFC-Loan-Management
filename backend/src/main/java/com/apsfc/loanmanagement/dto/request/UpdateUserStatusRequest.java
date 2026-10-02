package com.apsfc.loanmanagement.dto.request;

import com.apsfc.loanmanagement.enums.UserStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Admin toggling of an account's operability. LOCKED cannot be set via this
 * endpoint; only activation/deactivation is allowed.
 */
public record UpdateUserStatusRequest(
        @NotNull(message = "Status is required")
        UserStatus status
) {
}