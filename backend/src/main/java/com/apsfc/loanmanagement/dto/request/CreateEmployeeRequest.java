package com.apsfc.loanmanagement.dto.request;

import com.apsfc.loanmanagement.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Admin endpoint to onboard a staff member: creates the login User plus the
 * Employee record in one transaction.
 */
public record CreateEmployeeRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username may contain letters, digits, '_', '.', '-'")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 120, message = "Email must be at most 120 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be 8-72 characters")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
                message = "Password must contain uppercase, lowercase, digit and special character")
        String password,

        @NotBlank(message = "Full name is required")
        @Size(max = 120, message = "Full name must be at most 120 characters")
        String fullName,

        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Phone must be 10-15 digits")
        String phone,

        @NotNull(message = "Role is required")
        Role role,

        @NotBlank(message = "Employee code is required")
        @Size(max = 20, message = "Employee code must be at most 20 characters")
        String employeeCode,

        @Size(max = 60, message = "Department must be at most 60 characters")
        String department,

        @Size(max = 60, message = "Designation must be at most 60 characters")
        String designation,

        @PastOrPresent(message = "Joining date cannot be in the future")
        LocalDate joiningDate
) {
}