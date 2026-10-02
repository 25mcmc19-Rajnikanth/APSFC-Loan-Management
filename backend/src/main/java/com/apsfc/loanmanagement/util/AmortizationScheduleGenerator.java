package com.apsfc.loanmanagement.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds a monthly amortisation table (the EMI schedule) from date of
 * disbursement. The final installment is adjusted so the schedule closes the
 * principal exactly, and every monetary figure is rounded to 2 decimals.
 */
@Component
public class AmortizationScheduleGenerator {

    private static final int MONEY_SCALE = 2;

    public List<AmortizationRow> generate(BigDecimal principal, BigDecimal annualRatePercent,
                                          int months, BigDecimal emi, LocalDate startDate) {
        if (months <= 0) {
            throw new IllegalArgumentException("Tenure must be positive");
        }

        BigDecimal monthlyRate = annualRatePercent
                .divide(BigDecimal.valueOf(1200), 20, RoundingMode.HALF_EVEN);
        BigDecimal runningBalance = principal;
        List<AmortizationRow> rows = new ArrayList<>(months);

        for (int i = 1; i <= months; i++) {
            BigDecimal interest = runningBalance.multiply(monthlyRate)
                    .setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);

            BigDecimal principalPart;
            if (i == months) {
                principalPart = runningBalance;
            } else {
                principalPart = emi.subtract(interest);
                if (principalPart.signum() < 0) {
                    principalPart = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);
                } else if (principalPart.compareTo(runningBalance) > 0) {
                    principalPart = runningBalance;
                }
            }

            BigDecimal total = principalPart.add(interest);
            rows.add(new AmortizationRow(i, startDate.plusMonths(i),
                    principalPart, interest, total));

            if (i < months) {
                runningBalance = runningBalance.subtract(principalPart);
            }
        }
        return rows;
    }
}