package com.apsfc.loanmanagement.service;

import com.apsfc.loanmanagement.dto.response.CustomerResponse;
import com.apsfc.loanmanagement.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomerService {

    Page<CustomerResponse> list(UserStatus status, String search, Boolean kycVerified, Pageable pageable);

    CustomerResponse getById(Long id);

    CustomerResponse updateStatus(Long id, UserStatus status);

    CustomerResponse verifyKyc(Long id);
}