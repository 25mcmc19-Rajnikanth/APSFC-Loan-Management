package com.apsfc.loanmanagement.dto.response;

import com.apsfc.loanmanagement.enums.EmploymentType;
import com.apsfc.loanmanagement.enums.UserStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Customer profile. Aadhaar is masked to its last four digits. Loan aggregates
 * (active count, outstanding) are only filled on the detail view.
 */
public record CustomerResponse(
        Long id,
        Long userId,
        String username,
        String email,
        String fullName,
        String phone,
        String aadhaarNumberMasked,
        String panNumber,
        LocalDate dateOfBirth,
        String gender,
        EmploymentType employmentType,
        String city,
        String state,
        String pincode,
        boolean kycVerified,
        UserStatus status,
        Long activeLoanCount,
        BigDecimal outstandingBalance,
        LocalDateTime createdAt
) {
}