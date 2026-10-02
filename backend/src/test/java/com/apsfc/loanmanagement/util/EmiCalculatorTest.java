package com.apsfc.loanmanagement.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmiCalculatorTest {

    private final EmiCalculator calculator = new EmiCalculator();

    @Test
    void computesStandardEmiExample() {
        // Classic case: 1,00,000 at 12% p.a. for 12 months -> EMI 8884.88
        BigDecimal emi = calculator.computeEmi(
                new BigDecimal("100000"), new BigDecimal("12"), 12);
        assertThat(emi).isEqualByComparingTo("8884.88");
    }

    @Test
    void zeroInterestEmiIsSimpleDivision() {
        BigDecimal emi = calculator.computeEmi(new BigDecimal("120000"), BigDecimal.ZERO, 12);
        assertThat(emi).isEqualByComparingTo("10000.00");
    }

    @Test
    void emiRisesWithRateAtFixedTenure() {
        BigDecimal low = calculator.computeEmi(new BigDecimal("500000"), new BigDecimal("8.5"), 60);
        BigDecimal high = calculator.computeEmi(new BigDecimal("500000"), new BigDecimal("13.5"), 60);
        assertThat(high).isGreaterThan(low);
    }

    @Test
    void rejectsInvalidInput() {
        assertThatThrownBy(() -> calculator.computeEmi(BigDecimal.ZERO, new BigDecimal("10"), 12))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.computeEmi(new BigDecimal("10000"), new BigDecimal("10"), 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.computeEmi(
                new BigDecimal("10000"), new BigDecimal("-1"), 12))
                .isInstanceOf(IllegalArgumentException.class);
    }
}