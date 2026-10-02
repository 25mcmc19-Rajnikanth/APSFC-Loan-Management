package com.apsfc.loanmanagement.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Edits to a DRAFT application. Null fields are ignored; when amount/tenure/
 * product change, EMI is recomputed and product ranges revalidated.
 */
public record UpdateLoanApplicationRequest(
        Long productId,

        @DecimalMin(value = "0.01", message = "Loan amount must be positive")
        BigDecimal loanAmount,

        @Min(value = 1, message = "Tenure must be at least 1 month")
        @Max(value = 480, message = "Tenure cannot exceed 480 months")
        Integer tenureMonths,

        @Size(max = 255, message = "Purpose must be at most 255 characters")
        String purpose
) {
}