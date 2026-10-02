package com.apsfc.loanmanagement.dto.response;

import com.apsfc.loanmanagement.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * An installment that is due within a window (upcoming) or has passed its due
 * date unpaid (overdue).
 */
public record EmiDueResponse(
        Long loanId,
        String loanNumber,
        Long customerId,
        String customerName,
        Long emiId,
        Integer emiNumber,
        LocalDate dueDate,
        BigDecimal installmentAmount,
        BigDecimal cumulativePaid,
        BigDecimal remainingDue,
        PaymentStatus paymentStatus
) {
}