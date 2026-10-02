package com.apsfc.loanmanagement.dto.response;

import com.apsfc.loanmanagement.enums.LoanStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Loan representation for customers and staff. Outstanding balance is the
 * remaining contractual amount repayable.
 */
public record LoanResponse(
        Long id,
        String loanNumber,
        Long applicationId,
        String applicationNumber,
        Long customerId,
        String customerName,
        Long productId,
        String productName,
        BigDecimal principalAmount,
        BigDecimal interestRate,
        Integer tenureMonths,
        BigDecimal monthlyEmi,
        BigDecimal totalInterest,
        BigDecimal totalAmount,
        BigDecimal disbursedAmount,
        LocalDate disbursementDate,
        BigDecimal amountPaid,
        BigDecimal outstandingBalance,
        LoanStatus status,
        String disbursedByName,
        LocalDateTime closedAt,
        LocalDateTime createdAt
) {
}