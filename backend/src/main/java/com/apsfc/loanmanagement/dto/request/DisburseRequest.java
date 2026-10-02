package com.apsfc.loanmanagement.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PastOrPresent;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Disbursement of an APPROVED application. Both fields are optional - defaults
 * are the application principal and today's date.
 */
public record DisburseRequest(
        @DecimalMin(value = "0.01", message = "Disbursed amount must be positive")
        BigDecimal disbursedAmount,

        @PastOrPresent(message = "Disbursement date cannot be in the future")
        LocalDate disbursementDate
) {
}