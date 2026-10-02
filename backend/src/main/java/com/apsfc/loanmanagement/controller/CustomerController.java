package com.apsfc.loanmanagement.controller;

import com.apsfc.loanmanagement.dto.request.UpdateUserStatusRequest;
import com.apsfc.loanmanagement.dto.response.CustomerResponse;
import com.apsfc.loanmanagement.dto.response.PageResponse;
import com.apsfc.loanmanagement.enums.UserStatus;
import com.apsfc.loanmanagement.service.CustomerService;
import com.apsfc.loanmanagement.util.PageableUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/customers")
@Tag(name = "Customers", description = "Staff view and management of customer profiles")
public class CustomerController {

    private static final Set<String> SORTABLE = Set.of(
            "createdAt", "user.fullName", "user.username", "city");

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('LOAN_OFFICER','MANAGER','ADMIN')")
    @Operation(summary = "List customers (staff). Filters: status, search, kycVerified")
    public ResponseEntity<PageResponse<CustomerResponse>> list(
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean kycVerified,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String dir) {
        Pageable pageable = PageableUtil.of(page, size, sort, dir, "createdAt", SORTABLE);
        return ResponseEntity.ok(PageResponse.from(
                customerService.list(status, search, kycVerified, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER','MANAGER','ADMIN')")
    @Operation(summary = "Customer profile with active-loan aggregates")
    public ResponseEntity<CustomerResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getById(id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Operation(summary = "Activate / deactivate a customer account (MANAGER, ADMIN)")
    public ResponseEntity<CustomerResponse> updateStatus(
            @PathVariable Long id, @Valid @RequestBody UpdateUserStatusRequest request) {
        return ResponseEntity.ok(customerService.updateStatus(id, request.status()));
    }

    @PostMapping("/{id}/kyc/verify")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Operation(summary = "Mark a customer's KYC as verified (idempotent)")
    public ResponseEntity<CustomerResponse> verifyKyc(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.verifyKyc(id));
    }
}