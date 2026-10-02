package com.apsfc.loanmanagement.dto.request;

import com.apsfc.loanmanagement.enums.PaymentMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A repayment against a loan. Amount is applied to the next unpaid
 * installment only, so it cannot exceed that installment's remaining due.
 */
public record CreateRepaymentRequest(
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Payment amount must be positive")
        BigDecimal amount,

        @NotNull(message = "Payment mode is required")
        PaymentMode paymentMode,

        @Size(max = 60, message = "Transaction reference cannot exceed 60 characters")
        String transactionRef,

        @PastOrPresent(message = "Payment date cannot be in the future")
        LocalDate paymentDate,

        @Size(max = 255, message = "Remarks cannot exceed 255 characters")
        String remarks
) {
}