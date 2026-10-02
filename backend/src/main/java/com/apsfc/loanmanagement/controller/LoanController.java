package com.apsfc.loanmanagement.controller;

import com.apsfc.loanmanagement.dto.request.DisburseRequest;
import com.apsfc.loanmanagement.dto.response.EmiScheduleResponse;
import com.apsfc.loanmanagement.dto.response.LoanResponse;
import com.apsfc.loanmanagement.dto.response.PageResponse;
import com.apsfc.loanmanagement.enums.LoanStatus;
import com.apsfc.loanmanagement.service.LoanService;
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
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/loans")
@Tag(name = "Loans", description = "Disbursement, loan enquiry and EMI schedule")
public class LoanController {

    private static final Set<String> SORTABLE = Set.of(
            "loanNumber", "principalAmount", "interestRate", "tenureMonths",
            "monthlyEmi", "totalAmount", "outstandingBalance", "status", "createdAt");

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping("/disburse/{applicationId}")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Disburse an APPROVED application; creates loan + EMI schedule (MANAGER)")
    public ResponseEntity<LoanResponse> disburse(
            @PathVariable Long applicationId, @Valid @RequestBody DisburseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.disburse(applicationId, request));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "List the caller's loans (paginated, optional status filter)")
    public ResponseEntity<PageResponse<LoanResponse>> listMine(
            @RequestParam(required = false) LoanStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String dir) {
        Pageable pageable = PageableUtil.of(page, size, sort, dir, "createdAt", SORTABLE);
        return ResponseEntity.ok(PageResponse.from(loanService.listMine(status, pageable)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('LOAN_OFFICER','MANAGER','ADMIN')")
    @Operation(summary = "List all loans (staff). Filters: status, productId, dateFrom, dateTo")
    public ResponseEntity<PageResponse<LoanResponse>> listAll(
            @RequestParam(required = false) LoanStatus status,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String dir) {
        Pageable pageable = PageableUtil.of(page, size, sort, dir, "createdAt", SORTABLE);
        return ResponseEntity.ok(PageResponse.from(
                loanService.listAll(status, productId, dateFrom, dateTo, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single loan (owner or staff)")
    public ResponseEntity<LoanResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(loanService.getById(id));
    }

    @GetMapping("/{id}/emi")
    @Operation(summary = "Full EMI schedule for a loan (owner or staff)")
    public ResponseEntity<List<EmiScheduleResponse>> emiSchedule(@PathVariable Long id) {
        return ResponseEntity.ok(loanService.getEmiSchedule(id));
    }
}