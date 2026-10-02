package com.apsfc.loanmanagement.dto.response;

import com.apsfc.loanmanagement.enums.PaymentMode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A recorded repayment and the installment it was allocated to.
 */
public record RepaymentResponse(
        Long id,
        Long loanId,
        String loanNumber,
        Long emiAllocationId,
        Integer emiNumber,
        BigDecimal amount,
        PaymentMode paymentMode,
        String transactionRef,
        LocalDate paymentDate,
        String remarks,
        String recordedByName,
        LocalDateTime createdAt
) {
}