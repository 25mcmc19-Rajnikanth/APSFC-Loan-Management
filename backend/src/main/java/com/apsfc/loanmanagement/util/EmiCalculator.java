package com.apsfc.loanmanagement.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Pure financial calculation used for EMI and amortisation tables.
 *
 * EMI = P * r * (1+r)^n / ((1+r)^n - 1)
 *   P = principal, r = monthly rate (annual/12/100), n = months
 *
 * Everything is BigDecimal (never double/float) because binary floating point
 * cannot represent decimal money values exactly; rounding is applied at the
 * final 2-decimal "paisa" scale with HALF_EVEN.
 */
@Component
public class EmiCalculator {

    private static final MathContext MC = new MathContext(50);
    private static final int MONEY_SCALE = 2;

    public BigDecimal computeEmi(BigDecimal principal, BigDecimal annualRatePercent, int months) {
        if (principal == null || principal.signum() <= 0) {
            throw new IllegalArgumentException("Principal must be positive");
        }
        if (annualRatePercent == null || annualRatePercent.signum() < 0) {
            throw new IllegalArgumentException("Annual rate must be >= 0");
        }
        if (months <= 0) {
            throw new IllegalArgumentException("Tenure must be positive");
        }

        BigDecimal monthlyRate = annualRatePercent
                .divide(BigDecimal.valueOf(1200), 20, RoundingMode.HALF_EVEN);

        if (monthlyRate.signum() == 0) {
            return principal.divide(BigDecimal.valueOf(months), MONEY_SCALE, RoundingMode.HALF_EVEN);
        }

        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate, MC);
        BigDecimal powerTerm = onePlusRate.pow(months, MC);

        BigDecimal numerator = principal.multiply(monthlyRate, MC).multiply(powerTerm, MC);
        BigDecimal denominator = powerTerm.subtract(BigDecimal.ONE, MC);

        return numerator.divide(denominator, MONEY_SCALE, RoundingMode.HALF_EVEN);
    }
}