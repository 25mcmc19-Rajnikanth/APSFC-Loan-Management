package com.apsfc.loanmanagement.dto.response;

import java.math.BigDecimal;

/**
 * One row of the portfolio-by-product report (loans at a given status).
 */
public record ProductPortfolioRow(
        Long productId,
        String productName,
        Long loanCount,
        BigDecimal disbursed,
        BigDecimal outstanding
) {
}