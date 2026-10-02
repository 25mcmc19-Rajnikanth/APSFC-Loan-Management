package com.apsfc.loanmanagement.controller;

import com.apsfc.loanmanagement.dto.response.CollectionSummaryRow;
import com.apsfc.loanmanagement.dto.response.DashboardSummaryResponse;
import com.apsfc.loanmanagement.dto.response.EmiDueResponse;
import com.apsfc.loanmanagement.dto.response.ProductPortfolioRow;
import com.apsfc.loanmanagement.enums.LoanStatus;
import com.apsfc.loanmanagement.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@Validated
@Tag(name = "Dashboard & Reports", description = "Portfolio aggregates for staff")
@PreAuthorize("hasAnyRole('LOAN_OFFICER','MANAGER','ADMIN')")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard/summary")
    @Operation(summary = "Executive snapshot: customers, applications, portfolio, collections, EMI health")
    public ResponseEntity<DashboardSummaryResponse> summary() {
        return ResponseEntity.ok(dashboardService.summary());
    }

    @GetMapping("/reports/portfolio-by-product")
    @Operation(summary = "Disbursed and outstanding amounts per product at a given loan status (default ACTIVE)")
    public ResponseEntity<List<ProductPortfolioRow>> portfolioByProduct(
            @RequestParam(required = false) LoanStatus status) {
        return ResponseEntity.ok(dashboardService.portfolioByStatus(status));
    }

    @GetMapping("/reports/upcoming-emis")
    @Operation(summary = "Installments falling due within the next N days (default 30, max 365)")
    public ResponseEntity<List<EmiDueResponse>> upcomingEmis(
            @RequestParam(defaultValue = "30")
            @Min(1) @Max(365) int days) {
        return ResponseEntity.ok(dashboardService.upcomingEmis(days));
    }

    @GetMapping("/reports/overdue-emis")
    @Operation(summary = "Installments past their due date and not fully paid")
    public ResponseEntity<List<EmiDueResponse>> overdueEmis() {
        return ResponseEntity.ok(dashboardService.overdueEmis());
    }

    @GetMapping("/reports/collections")
    @Operation(summary = "Collections by payment mode. Defaults to the last 30 days if no range given")
    public ResponseEntity<List<CollectionSummaryRow>> collections(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ResponseEntity.ok(dashboardService.collectionSummary(dateFrom, dateTo));
    }
}