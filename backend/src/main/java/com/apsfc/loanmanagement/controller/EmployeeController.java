package com.apsfc.loanmanagement.controller;

import com.apsfc.loanmanagement.dto.request.CreateEmployeeRequest;
import com.apsfc.loanmanagement.dto.request.UpdateEmployeeRequest;
import com.apsfc.loanmanagement.dto.request.UpdateUserStatusRequest;
import com.apsfc.loanmanagement.dto.response.EmployeeResponse;
import com.apsfc.loanmanagement.dto.response.PageResponse;
import com.apsfc.loanmanagement.enums.Role;
import com.apsfc.loanmanagement.enums.UserStatus;
import com.apsfc.loanmanagement.service.EmployeeService;
import com.apsfc.loanmanagement.util.PageableUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/employees")
@Tag(name = "Employees", description = "Staff directory and administration (ADMIN mutations)")
public class EmployeeController {

    private static final Set<String> SORTABLE = Set.of(
            "createdAt", "employeeCode", "user.fullName");

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Provision a staff account (LOAN_OFFICER or MANAGER) - ADMIN")
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody CreateEmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('LOAN_OFFICER','MANAGER','ADMIN')")
    @Operation(summary = "List employees. Filters: role, status, search")
    public ResponseEntity<PageResponse<EmployeeResponse>> list(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String dir) {
        Pageable pageable = PageableUtil.of(page, size, sort, dir, "createdAt", SORTABLE);
        return ResponseEntity.ok(PageResponse.from(employeeService.list(role, status, search, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER','MANAGER','ADMIN')")
    @Operation(summary = "Employee detail")
    public ResponseEntity<EmployeeResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update department / designation / joining date / phone - ADMIN")
    public ResponseEntity<EmployeeResponse> update(
            @PathVariable Long id, @Valid @RequestBody UpdateEmployeeRequest request) {
        return ResponseEntity.ok(employeeService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate / deactivate an employee account - ADMIN")
    public ResponseEntity<EmployeeResponse> updateStatus(
            @PathVariable Long id, @Valid @RequestBody UpdateUserStatusRequest request) {
        return ResponseEntity.ok(employeeService.updateStatus(id, request.status()));
    }
}