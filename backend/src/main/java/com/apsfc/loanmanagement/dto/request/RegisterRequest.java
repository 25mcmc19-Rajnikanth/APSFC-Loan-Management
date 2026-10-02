package com.apsfc.loanmanagement.dto.request;

import com.apsfc.loanmanagement.enums.EmploymentType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.LocalDate;

/**
 * Customer self-registration. Creates the login User (role CUSTOMER) plus the
 * Customer KYC profile in a single transaction.
 */
@Builder
public record RegisterRequest(
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

        @NotBlank(message = "Aadhaar number is required")
        @Pattern(regexp = "^[0-9]{12}$", message = "Aadhaar must be exactly 12 digits")
        String aadhaarNumber,

        @NotBlank(message = "PAN is required")
        @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]$", message = "PAN must match AAA999999A format")
        String panNumber,

        @NotNull(message = "Date of birth is required")
        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth,

        @Size(max = 10, message = "Gender must be at most 10 characters")
        String gender,

        EmploymentType employmentType,

        @Size(max = 200)
        String addressLine1,

        @Size(max = 200)
        String addressLine2,

        @Size(max = 60)
        String city,

        @Size(max = 60)
        String state,

        @Size(max = 10)
        @Pattern(regexp = "^[0-9]{6}$", message = "Pincode must be 6 digits")
        String pincode
) {
}