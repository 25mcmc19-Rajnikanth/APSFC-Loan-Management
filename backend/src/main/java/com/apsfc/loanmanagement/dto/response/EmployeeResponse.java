package com.apsfc.loanmanagement.dto.response;

import com.apsfc.loanmanagement.enums.Role;
import com.apsfc.loanmanagement.enums.UserStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmployeeResponse(
        Long id,
        Long userId,
        String username,
        String email,
        String fullName,
        String phone,
        Role role,
        String employeeCode,
        String designation,
        String department,
        UserStatus status,
        LocalDate joiningDate,
        LocalDateTime createdAt
) {
}