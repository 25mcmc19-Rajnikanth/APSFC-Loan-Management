package com.apsfc.loanmanagement.dto.request;

import com.apsfc.loanmanagement.enums.ProductStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * PUT update: null fields are ignored (existing value kept), non-null fields
 * replace the current value. Cross-field ranges are validated on the merged
 * product in the service layer.
 */
public record UpdateLoanProductRequest(
        @Size(max = 120, message = "Product name must be at most 120 characters")
        String name,

        @Size(max = 30, message = "Product code must be at most 30 characters")
        String code,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,

        @DecimalMin(value = "0.01", message = "Minimum amount must be positive")
        BigDecimal minAmount,

        @DecimalMin(value = "0.01", message = "Maximum amount must be positive")
        BigDecimal maxAmount,

        @DecimalMin(value = "0.01", message = "Interest rate must be positive")
        @DecimalMax(value = "100.00", message = "Interest rate cannot exceed 100%")
        BigDecimal baseInterestRate,

        @Min(value = 1, message = "Minimum tenure must be at least 1 month")
        Integer minTenureMonths,

        @Min(value = 1, message = "Maximum tenure must be at least 1 month")
        @Max(value = 480, message = "Maximum tenure cannot exceed 480 months")
        Integer maxTenureMonths,

        @Size(max = 500, message = "Eligibility criteria must be at most 500 characters")
        String eligibilityCriteria,

        ProductStatus status
) {
}