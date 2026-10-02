package com.apsfc.loanmanagement.controller;

import com.apsfc.loanmanagement.dto.request.CreateLoanApplicationRequest;
import com.apsfc.loanmanagement.dto.request.DecisionRequest;
import com.apsfc.loanmanagement.dto.request.RecommendRequest;
import com.apsfc.loanmanagement.dto.request.UpdateLoanApplicationRequest;
import com.apsfc.loanmanagement.dto.response.ApplicationHistoryResponse;
import com.apsfc.loanmanagement.dto.response.LoanApplicationResponse;
import com.apsfc.loanmanagement.dto.response.PageResponse;
import com.apsfc.loanmanagement.enums.ApplicationStatus;
import com.apsfc.loanmanagement.service.LoanApplicationService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/loan-applications")
@Tag(name = "Loan Applications", description = "Customer application workflow + staff review/decision")
public class LoanApplicationController {

    private static final Set<String> SORTABLE = Set.of(
            "applicationNumber", "loanAmount", "status", "createdAt");

    private final LoanApplicationService loanApplicationService;

    public LoanApplicationController(LoanApplicationService loanApplicationService) {
        this.loanApplicationService = loanApplicationService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Create a draft loan application (CUSTOMER)")
    public ResponseEntity<LoanApplicationResponse> create(
            @Valid @RequestBody CreateLoanApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanApplicationService.create(request));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "List the caller's applications (paginated, optional status filter)")
    public ResponseEntity<PageResponse<LoanApplicationResponse>> listMine(
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String dir) {
        Pageable pageable = PageableUtil.of(page, size, sort, dir, "createdAt", SORTABLE);
        return ResponseEntity.ok(PageResponse.from(loanApplicationService.listMine(status, pageable)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('LOAN_OFFICER','MANAGER','ADMIN')")
    @Operation(summary = "List all applications (staff). Filters: status, productId, dateFrom, dateTo")
    public ResponseEntity<PageResponse<LoanApplicationResponse>> listAll(
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String dir) {
        Pageable pageable = PageableUtil.of(page, size, sort, dir, "createdAt", SORTABLE);
        return ResponseEntity.ok(PageResponse.from(
                loanApplicationService.listAll(status, productId, dateFrom, dateTo, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single application (owner or staff)")
    public ResponseEntity<LoanApplicationResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(loanApplicationService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Edit a DRAFT application (owner only)")
    public ResponseEntity<LoanApplicationResponse> updateDraft(
            @PathVariable Long id, @Valid @RequestBody UpdateLoanApplicationRequest request) {
        return ResponseEntity.ok(loanApplicationService.updateDraft(id, request));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Submit a DRAFT application (owner only)")
    public ResponseEntity<LoanApplicationResponse> submit(@PathVariable Long id) {
        return ResponseEntity.ok(loanApplicationService.submit(id));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Cancel/withdraw a DRAFT or SUBMITTED application (owner only)")
    public ResponseEntity<LoanApplicationResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(loanApplicationService.cancel(id));
    }

    @PostMapping("/{id}/start-review")
    @PreAuthorize("hasRole('LOAN_OFFICER')")
    @Operation(summary = "Begin verification of a SUBMITTED application (LOAN_OFFICER)")
    public ResponseEntity<LoanApplicationResponse> startReview(@PathVariable Long id) {
        return ResponseEntity.ok(loanApplicationService.startReview(id));
    }

    @PostMapping("/{id}/recommend")
    @PreAuthorize("hasRole('LOAN_OFFICER')")
    @Operation(summary = "Advisory approve/reject recommendation (LOAN_OFFICER, non-binding)")
    public ResponseEntity<LoanApplicationResponse> recommend(
            @PathVariable Long id, @Valid @RequestBody RecommendRequest request) {
        return ResponseEntity.ok(loanApplicationService.recommend(id, request));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Final approval (MANAGER)")
    public ResponseEntity<LoanApplicationResponse> approve(
            @PathVariable Long id, @Valid @RequestBody DecisionRequest request) {
        return ResponseEntity.ok(loanApplicationService.approve(id, request));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Reject an application (MANAGER)")
    public ResponseEntity<LoanApplicationResponse> reject(
            @PathVariable Long id, @Valid @RequestBody DecisionRequest request) {
        return ResponseEntity.ok(loanApplicationService.reject(id, request));
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "State-transition audit trail (owner or staff)")
    public ResponseEntity<List<ApplicationHistoryResponse>> history(@PathVariable Long id) {
        return ResponseEntity.ok(loanApplicationService.getHistory(id));
    }
}