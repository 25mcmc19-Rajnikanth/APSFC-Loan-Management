package com.apsfc.loanmanagement.dto.response;

import java.math.BigDecimal;

/**
 * Aggregate of active loans per customer, used to enrich the customer list
 * without N+1 queries.
 */
public record CustomerLoanAggregate(
        Long customerId,
        Long activeLoanCount,
        BigDecimal outstandingBalance
) {
}