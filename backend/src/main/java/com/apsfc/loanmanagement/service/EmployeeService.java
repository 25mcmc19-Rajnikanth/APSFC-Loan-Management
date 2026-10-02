package com.apsfc.loanmanagement.service;

import com.apsfc.loanmanagement.dto.request.CreateEmployeeRequest;
import com.apsfc.loanmanagement.dto.request.UpdateEmployeeRequest;
import com.apsfc.loanmanagement.dto.response.EmployeeResponse;
import com.apsfc.loanmanagement.enums.Role;
import com.apsfc.loanmanagement.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EmployeeService {

    EmployeeResponse create(CreateEmployeeRequest request);

    EmployeeResponse update(Long id, UpdateEmployeeRequest request);

    EmployeeResponse getById(Long id);

    Page<EmployeeResponse> list(Role role, UserStatus status, String search, Pageable pageable);

    EmployeeResponse updateStatus(Long id, UserStatus status);
}