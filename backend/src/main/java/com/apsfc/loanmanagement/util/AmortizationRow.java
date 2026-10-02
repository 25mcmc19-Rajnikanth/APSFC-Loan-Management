package com.apsfc.loanmanagement.util;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One installment row of a generated amortisation schedule.
 *
 * @param principalComponent how much principal this installment repays
 * @param interestComponent  how much interest this installment covers
 * @param totalAmount        principal + interest for the installment
 */
public record AmortizationRow(int emiNumber, LocalDate dueDate,
                              BigDecimal principalComponent, BigDecimal interestComponent,
                              BigDecimal totalAmount) {
}