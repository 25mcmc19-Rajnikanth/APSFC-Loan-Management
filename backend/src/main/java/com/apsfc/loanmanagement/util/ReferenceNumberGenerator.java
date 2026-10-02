package com.apsfc.loanmanagement.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates human-readable, low-collision reference numbers. Uniqueness is
 * ultimately protected by DB unique constraints.
 */
@Component
public class ReferenceNumberGenerator {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    public String nextApplicationNumber() {
        return "APP-" + LocalDate.now().format(DATE) + "-" + randomSuffix();
    }

    public String nextLoanNumber() {
        return "LN-" + LocalDate.now().format(DATE) + "-" + randomSuffix();
    }

    private String randomSuffix() {
        return "%06d".formatted(ThreadLocalRandom.current().nextInt(1_000_000));
    }
}