package com.apsfc.loanmanagement.dto.request;

import jakarta.validation.constraints.Size;

/**
 * MANAGER final decision. Comment is strongly encouraged for audit trail.
 */
public record DecisionRequest(
        @Size(max = 500, message = "Comment must be at most 500 characters")
        String comment
) {
}