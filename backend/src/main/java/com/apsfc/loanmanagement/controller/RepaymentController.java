package com.apsfc.loanmanagement.controller;

import com.apsfc.loanmanagement.dto.request.CreateRepaymentRequest;
import com.apsfc.loanmanagement.dto.response.PageResponse;
import com.apsfc.loanmanagement.dto.response.RepaymentResponse;
import com.apsfc.loanmanagement.service.RepaymentService;
import com.apsfc.loanmanagement.util.PageableUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Set;

@RestController
@RequestMapping("/api")
@Tag(name = "Repayments", description = "Installment payments and repayment history")
public class RepaymentController {

    private static final Set<String> SORTABLE = Set.of(
            "paymentDate", "amount", "createdAt");

    private final RepaymentService repaymentService;

    public RepaymentController(RepaymentService repaymentService) {
        this.repaymentService = repaymentService;
    }

    @PostMapping("/loans/{loanId}/repayments")
    @PreAuthorize("hasAnyRole('CUSTOMER','LOAN_OFFICER','MANAGER','ADMIN')")
    @Operation(summary = "Record a payment against the next unpaid installment",
            description = "Owner customer or any staff. Amount cannot exceed the current installment's remaining due.")
    public ResponseEntity<RepaymentResponse> record(
            @PathVariable Long loanId, @Valid @RequestBody CreateRepaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(repaymentService.record(loanId, request));
    }

    @GetMapping("/loans/{loanId}/repayments")
    @Operation(summary = "Repayment history for a specific loan (owner or staff)")
    public ResponseEntity<PageResponse<RepaymentResponse>> listByLoan(
            @PathVariable Long loanId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "paymentDate") String sort,
            @RequestParam(defaultValue = "desc") String dir) {
        Pageable pageable = PageableUtil.of(page, size, sort, dir, "createdAt", SORTABLE);
        return ResponseEntity.ok(PageResponse.from(repaymentService.listByLoan(loanId, pageable)));
    }

    @GetMapping("/repayments/mine")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "All repayment records across the caller's loans (paginated)")
    public ResponseEntity<PageResponse<RepaymentResponse>> listMine(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "paymentDate") String sort,
            @RequestParam(defaultValue = "desc") String dir) {
        Pageable pageable = PageableUtil.of(page, size, sort, dir, "createdAt", SORTABLE);
        return ResponseEntity.ok(PageResponse.from(repaymentService.listMine(dateFrom, dateTo, pageable)));
    }

    @GetMapping("/repayments")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER','MANAGER','ADMIN')")
    @Operation(summary = "All repayments (staff). Filters: loanId, dateFrom, dateTo")
    public ResponseEntity<PageResponse<RepaymentResponse>> listAll(
            @RequestParam(required = false) Long loanId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "paymentDate") String sort,
            @RequestParam(defaultValue = "desc") String dir) {
        Pageable pageable = PageableUtil.of(page, size, sort, dir, "createdAt", SORTABLE);
        return ResponseEntity.ok(PageResponse.from(repaymentService.listAll(loanId, dateFrom, dateTo, pageable)));
    }
}