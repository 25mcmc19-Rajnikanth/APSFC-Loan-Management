package com.apsfc.loanmanagement.dto.request;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Updatable employment details. Null fields are left unchanged.
 */
public record UpdateEmployeeRequest(
        @Size(max = 60, message = "Department must be at most 60 characters")
        String department,

        @Size(max = 60, message = "Designation must be at most 60 characters")
        String designation,

        @PastOrPresent(message = "Joining date cannot be in the future")
        LocalDate joiningDate,
        String phone
) {
}