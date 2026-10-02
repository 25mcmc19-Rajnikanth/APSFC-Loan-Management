package com.apsfc.loanmanagement.dto.response;

import com.apsfc.loanmanagement.enums.ApplicationStatus;

/**
 * Applications count grouped by status (application funnel).
 */
public record StatusCount(
        ApplicationStatus status,
        Long count
) {
}