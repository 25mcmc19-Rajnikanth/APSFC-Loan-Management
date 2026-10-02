package com.apsfc.loanmanagement.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * Fields required to create a configurable loan product. Cross-field range
 * rules (max >= min, etc.) are enforced in the service layer and cross-checked
 * against the compile-time validations here.
 */
@Builder
public record CreateLoanProductRequest(
        @NotBlank(message = "Product name is required")
        @Size(max = 120, message = "Product name must be at most 120 characters")
        String name,

        @NotBlank(message = "Product code is required")
        @Size(max = 30, message = "Product code must be at most 30 characters")
        @Pattern(regexp = "^[A-Z0-9_]+$", message = "Code may contain uppercase letters, digits, '_'")
        String code,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,

        @NotNull(message = "Minimum amount is required")
        @DecimalMin(value = "0.01", message = "Minimum amount must be positive")
        BigDecimal minAmount,

        @NotNull(message = "Maximum amount is required")
        @DecimalMin(value = "0.01", message = "Maximum amount must be positive")
        BigDecimal maxAmount,

        @NotNull(message = "Interest rate is required")
        @DecimalMin(value = "0.01", message = "Interest rate must be positive")
        @DecimalMax(value = "100.00", message = "Interest rate cannot exceed 100%")
        BigDecimal baseInterestRate,

        @NotNull(message = "Minimum tenure is required")
        @Min(value = 1, message = "Minimum tenure must be at least 1 month")
        Integer minTenureMonths,

        @NotNull(message = "Maximum tenure is required")
        @Min(value = 1, message = "Maximum tenure must be at least 1 month")
        @Max(value = 480, message = "Maximum tenure cannot exceed 480 months")
        Integer maxTenureMonths,

        @Size(max = 500, message = "Eligibility criteria must be at most 500 characters")
        String eligibilityCriteria
) {
}