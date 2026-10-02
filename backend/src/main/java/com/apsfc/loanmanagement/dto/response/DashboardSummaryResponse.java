package com.apsfc.loanmanagement.dto.response;

import com.apsfc.loanmanagement.enums.ApplicationStatus;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Executive dashboard snapshot over the whole portfolio.
 */
public record DashboardSummaryResponse(
        long totalCustomers,
        long totalApplications,
        long totalLoans,
        long activeLoans,
        long closedLoans,
        BigDecimal totalOutstanding,
        BigDecimal totalDisbursed,
        BigDecimal disbursedThisMonth,
        BigDecimal totalCollections,
        BigDecimal collectionsThisMonth,
        long upcomingEmiCount,
        long overdueEmiCount,
        Map<ApplicationStatus, Long> applicationsByStatus
) {
}