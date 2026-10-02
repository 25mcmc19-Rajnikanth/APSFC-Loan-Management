package com.apsfc.loanmanagement.mapper;

import com.apsfc.loanmanagement.dto.response.CustomerResponse;
import com.apsfc.loanmanagement.entity.Customer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CustomerMapper {

    public CustomerResponse toResponse(Customer customer,
                                       Long activeLoanCount, BigDecimal outstandingBalance) {
        return new CustomerResponse(
                customer.getId(),
                customer.getUser().getId(),
                customer.getUser().getUsername(),
                customer.getUser().getEmail(),
                customer.getUser().getFullName(),
                customer.getUser().getPhone(),
                maskAadhaar(customer.getAadhaarNumber()),
                customer.getPanNumber(),
                customer.getDateOfBirth(),
                customer.getGender(),
                customer.getEmploymentType(),
                customer.getCity(),
                customer.getState(),
                customer.getPincode(),
                customer.isKycVerified(),
                customer.getUser().getStatus(),
                activeLoanCount,
                outstandingBalance,
                customer.getCreatedAt());
    }

    private String maskAadhaar(String aadhaar) {
        if (aadhaar == null || aadhaar.length() < 4) {
            return aadhaar;
        }
        return "XXXXXXXX" + aadhaar.substring(aadhaar.length() - 4);
    }
}