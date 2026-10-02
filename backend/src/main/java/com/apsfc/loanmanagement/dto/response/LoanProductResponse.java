package com.apsfc.loanmanagement.dto.response;

import com.apsfc.loanmanagement.enums.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Safe representation of a loan product for API consumers.
 */
public record LoanProductResponse(
        Long id,
        String name,
        String code,
        String description,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        BigDecimal baseInterestRate,
        Integer minTenureMonths,
        Integer maxTenureMonths,
        String eligibilityCriteria,
        ProductStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}