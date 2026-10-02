package com.apsfc.loanmanagement.mapper;

import com.apsfc.loanmanagement.dto.response.EmployeeResponse;
import com.apsfc.loanmanagement.entity.Employee;
import org.springframework.stereotype.Component;

@Component
public class EmployeeMapper {

    public EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getUser().getId(),
                employee.getUser().getUsername(),
                employee.getUser().getEmail(),
                employee.getUser().getFullName(),
                employee.getUser().getPhone(),
                employee.getUser().getRole(),
                employee.getEmployeeCode(),
                employee.getDesignation(),
                employee.getDepartment(),
                employee.getStatus(),
                employee.getJoiningDate(),
                employee.getCreatedAt());
    }
}