package com.apsfc.loanmanagement.dto.response;

import com.apsfc.loanmanagement.enums.PaymentMode;

import java.math.BigDecimal;

/**
 * Collections grouped by payment mode for a date window.
 */
public record CollectionSummaryRow(
        PaymentMode paymentMode,
        Long paymentCount,
        BigDecimal totalCollected
) {
}