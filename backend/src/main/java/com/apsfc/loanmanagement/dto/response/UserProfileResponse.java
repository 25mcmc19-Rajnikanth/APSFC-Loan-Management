package com.apsfc.loanmanagement.dto.response;

import com.apsfc.loanmanagement.enums.UserStatus;

/**
 * Response for GET /api/auth/me - adds the linked customer/employee id so the
 * frontend can route into the role-specific dashboard.
 */
public record UserProfileResponse(
        Long id,
        String username,
        String email,
        String fullName,
        String phone,
        String role,
        UserStatus status,
        Long customerId,
        Long employeeId
) {
}