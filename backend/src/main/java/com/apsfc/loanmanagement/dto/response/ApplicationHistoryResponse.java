package com.apsfc.loanmanagement.dto.response;

import java.time.LocalDateTime;

/**
 * One audit entry in an application's state-history trail.
 */
public record ApplicationHistoryResponse(
        Long id,
        String fromStatus,
        String toStatus,
        String action,
        String comment,
        String performedByName,
        LocalDateTime performedAt
) {
}