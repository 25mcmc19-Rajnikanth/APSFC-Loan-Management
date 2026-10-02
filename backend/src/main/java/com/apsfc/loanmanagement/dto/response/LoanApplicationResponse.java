package com.apsfc.loanmanagement.dto.response;

import com.apsfc.loanmanagement.enums.ApplicationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Loan application representation for both the owning customer and staff.
 * Names/flat ids are pre-joined by the mapper so clients do not need nested
 * objects.
 */
public record LoanApplicationResponse(
        Long id,
        String applicationNumber,
        ApplicationStatus status,
        Long customerId,
        String customerName,
        Long productId,
        String productName,
        BigDecimal loanAmount,
        Integer tenureMonths,
        BigDecimal interestRate,
        BigDecimal monthlyEmi,
        String purpose,
        String reviewComment,
        String decisionComment,
        String submittedByName,
        String reviewedByName,
        String approvedByName,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt,
        LocalDateTime decidedAt,
        LocalDateTime createdAt
) {
}