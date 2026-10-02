package com.apsfc.loanmanagement.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Dev bootstrap accounts created on first boot (only when missing).
 * Override in .env/env; defaults exist only for local development and are
 * logged as a warning when used.
 */
@ConfigurationProperties(prefix = "app.bootstrap")
public record BootstrapProperties(
        boolean enabled,
        AdminAccount admin,
        StaffAccount manager,
        StaffAccount officer
) {
    public record AdminAccount(String username, String email, String password, String fullName) {
    }

    public record StaffAccount(String username, String email, String password, String fullName,
                               String employeeCode, String department, String designation) {
    }
}