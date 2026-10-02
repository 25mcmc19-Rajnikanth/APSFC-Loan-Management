package com.apsfc.loanmanagement.dto.response;

import com.apsfc.loanmanagement.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One installment row of the EMI schedule.
 */
public record EmiScheduleResponse(
        Long id,
        Integer emiNumber,
        LocalDate dueDate,
        BigDecimal principalComponent,
        BigDecimal interestComponent,
        BigDecimal totalAmount,
        BigDecimal cumulativePaid,
        PaymentStatus paymentStatus,
        LocalDate paidDate
) {
}