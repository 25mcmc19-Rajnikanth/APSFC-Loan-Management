package com.apsfc.loanmanagement.service;

import com.apsfc.loanmanagement.dto.response.CollectionSummaryRow;
import com.apsfc.loanmanagement.dto.response.DashboardSummaryResponse;
import com.apsfc.loanmanagement.dto.response.EmiDueResponse;
import com.apsfc.loanmanagement.dto.response.ProductPortfolioRow;
import com.apsfc.loanmanagement.enums.LoanStatus;

import java.time.LocalDate;
import java.util.List;

public interface DashboardService {

    DashboardSummaryResponse summary();

    List<ProductPortfolioRow> portfolioByStatus(LoanStatus status);

    List<EmiDueResponse> upcomingEmis(int days);

    List<EmiDueResponse> overdueEmis();

    List<CollectionSummaryRow> collectionSummary(LocalDate from, LocalDate to);
}