package com.apsfc.loanmanagement.controller;

import com.apsfc.loanmanagement.dto.request.CreateLoanProductRequest;
import com.apsfc.loanmanagement.dto.request.UpdateLoanProductRequest;
import com.apsfc.loanmanagement.dto.response.LoanProductResponse;
import com.apsfc.loanmanagement.dto.response.PageResponse;
import com.apsfc.loanmanagement.enums.ProductStatus;
import com.apsfc.loanmanagement.service.LoanProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loan-products")
@Tag(name = "Loan Products", description = "Configurable loan products (view for all users, manage for ADMIN)")
public class LoanProductController {

    private final LoanProductService loanProductService;

    public LoanProductController(LoanProductService loanProductService) {
        this.loanProductService = loanProductService;
    }

    @GetMapping
    @Operation(summary = "List loan products (paginated, optional status filter)")
    public ResponseEntity<PageResponse<LoanProductResponse>> list(
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sort) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(sort));
        return ResponseEntity.ok(PageResponse.from(loanProductService.list(status, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single loan product")
    public ResponseEntity<LoanProductResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(loanProductService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a loan product (ADMIN only)")
    public ResponseEntity<LoanProductResponse> create(@Valid @RequestBody CreateLoanProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanProductService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a loan product (ADMIN only). Null fields keep current values.")
    public ResponseEntity<LoanProductResponse> update(@PathVariable Long id,
                                                      @Valid @RequestBody UpdateLoanProductRequest request) {
        return ResponseEntity.ok(loanProductService.update(id, request));
    }
}