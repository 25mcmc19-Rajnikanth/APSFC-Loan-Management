package com.apsfc.loanmanagement.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AmortizationScheduleGeneratorTest {

    private final AmortizationScheduleGenerator generator = new AmortizationScheduleGenerator();

    @Test
    void scheduleRepaysPrincipalExactly() {
        BigDecimal principal = new BigDecimal("100000");
        BigDecimal emi = new EmiCalculator().computeEmi(principal, new BigDecimal("12"), 12);
        List<AmortizationRow> rows = generator.generate(principal, new BigDecimal("12"), 12, emi,
                LocalDate.of(2026, 9, 18));

        assertThat(rows).hasSize(12);
        BigDecimal sumPrincipal = rows.stream()
                .map(AmortizationRow::principalComponent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sumPrincipal).isEqualByComparingTo(principal);

        BigDecimal sumTotal = rows.stream()
                .map(AmortizationRow::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sumTotal).isGreaterThanOrEqualTo(principal);
    }

    @Test
    void firstMonthInterestIsPrincipalTimesMonthlyRate() {
        List<AmortizationRow> rows = generator.generate(
                new BigDecimal("100000"), new BigDecimal("12"), 12,
                new BigDecimal("8884.88"), LocalDate.of(2026, 9, 18));
        assertThat(rows.get(0).interestComponent()).isEqualByComparingTo("1000.00");
        assertThat(rows.get(0).principalComponent()).isEqualByComparingTo("7884.88");
    }

    @Test
    void lastInstallmentClosesTheLoan() {
        List<AmortizationRow> rows = generator.generate(
                new BigDecimal("100000"), new BigDecimal("12"), 12,
                new BigDecimal("8884.88"), LocalDate.of(2026, 9, 18));

        BigDecimal balanceBeforeLast = rows.stream()
                .limit(11)
                .map(AmortizationRow::principalComponent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remaining = new BigDecimal("100000").subtract(balanceBeforeLast);
        assertThat(rows.get(11).principalComponent()).isEqualByComparingTo(remaining);
    }

    @Test
    void zeroInterestSplitsPrincipalEvenly() {
        List<AmortizationRow> rows = generator.generate(
                new BigDecimal("120000"), BigDecimal.ZERO, 12,
                new BigDecimal("10000"), LocalDate.of(2026, 9, 18));
        assertThat(rows).allMatch(r -> r.interestComponent()
                .compareTo(BigDecimal.ZERO) == 0);
        assertThat(rows.stream().map(AmortizationRow::principalComponent)
                .reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("120000");
        assertThat(rows.get(11).principalComponent()).isEqualByComparingTo("10000");
    }

    @Test
    void dueDatesIncrementMonthlyFromStart() {
        List<AmortizationRow> rows = generator.generate(
                new BigDecimal("100000"), new BigDecimal("10"), 3,
                new BigDecimal("34000"), LocalDate.of(2026, 1, 15));
        assertThat(rows.get(0).dueDate()).isEqualTo(LocalDate.of(2026, 2, 15));
        assertThat(rows.get(1).dueDate()).isEqualTo(LocalDate.of(2026, 3, 15));
        assertThat(rows.get(2).dueDate()).isEqualTo(LocalDate.of(2026, 4, 15));
    }

    @Test
    void rejectsNonPositiveTenure() {
        assertThatThrownBy(() -> generator.generate(
                new BigDecimal("100000"), new BigDecimal("10"), 0,
                new BigDecimal("10000"), LocalDate.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}