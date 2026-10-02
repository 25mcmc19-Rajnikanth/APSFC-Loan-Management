package com.apsfc.loanmanagement.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Customer creates a draft loan application. Cross-checks against the chosen
 * product's amount/tenure range happen in the service layer.
 */
public record CreateLoanApplicationRequest(
        @NotNull(message = "Product is required")
        Long productId,

        @NotNull(message = "Loan amount is required")
        @DecimalMin(value = "0.01", message = "Loan amount must be positive")
        BigDecimal loanAmount,

        @NotNull(message = "Tenure is required")
        @Min(value = 1, message = "Tenure must be at least 1 month")
        @Max(value = 480, message = "Tenure cannot exceed 480 months")
        Integer tenureMonths,

        @Size(max = 255, message = "Purpose must be at most 255 characters")
        String purpose
) {
}