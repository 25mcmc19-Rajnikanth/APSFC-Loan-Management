package com.apsfc.loanmanagement.service.impl;

import com.apsfc.loanmanagement.dto.response.CollectionSummaryRow;
import com.apsfc.loanmanagement.dto.response.DashboardSummaryResponse;
import com.apsfc.loanmanagement.dto.response.EmiDueResponse;
import com.apsfc.loanmanagement.dto.response.ProductPortfolioRow;
import com.apsfc.loanmanagement.dto.response.StatusCount;
import com.apsfc.loanmanagement.enums.ApplicationStatus;
import com.apsfc.loanmanagement.enums.LoanStatus;
import com.apsfc.loanmanagement.enums.PaymentStatus;
import com.apsfc.loanmanagement.repository.CustomerRepository;
import com.apsfc.loanmanagement.repository.EmiScheduleRepository;
import com.apsfc.loanmanagement.repository.LoanApplicationRepository;
import com.apsfc.loanmanagement.repository.LoanRepository;
import com.apsfc.loanmanagement.repository.RepaymentRepository;
import com.apsfc.loanmanagement.service.DashboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final List<PaymentStatus> UNPAID_STATUSES = List.of(
            PaymentStatus.PENDING, PaymentStatus.PARTIALLY_PAID);

    private final CustomerRepository customerRepository;
    private final LoanApplicationRepository applicationRepository;
    private final LoanRepository loanRepository;
    private final RepaymentRepository repaymentRepository;
    private final EmiScheduleRepository emiScheduleRepository;

    public DashboardServiceImpl(CustomerRepository customerRepository,
                                LoanApplicationRepository applicationRepository,
                                LoanRepository loanRepository,
                                RepaymentRepository repaymentRepository,
                                EmiScheduleRepository emiScheduleRepository) {
        this.customerRepository = customerRepository;
        this.applicationRepository = applicationRepository;
        this.loanRepository = loanRepository;
        this.repaymentRepository = repaymentRepository;
        this.emiScheduleRepository = emiScheduleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate deadline = today.plusDays(30);

        List<EmiDueResponse> upcoming = emiScheduleRepository.findDueBetween(
                today, deadline, UNPAID_STATUSES);
        List<EmiDueResponse> overdue = emiScheduleRepository.findDueBefore(today, UNPAID_STATUSES);

        Map<ApplicationStatus, Long> funnel = new EnumMap<>(ApplicationStatus.class);
        applicationRepository.countByStatusGrouped()
                .forEach(sc -> funnel.put(sc.status(), sc.count()));
        Arrays.stream(ApplicationStatus.values())
                .forEach(s -> funnel.putIfAbsent(s, 0L));

        return new DashboardSummaryResponse(
                customerRepository.count(),
                applicationRepository.count(),
                loanRepository.count(),
                loanRepository.countByStatus(LoanStatus.ACTIVE),
                loanRepository.countByStatus(LoanStatus.CLOSED),
                loanRepository.sumOutstandingByStatus(LoanStatus.ACTIVE),
                loanRepository.sumPrincipal(),
                loanRepository.sumPrincipalBetween(monthStart, today),
                repaymentRepository.sumAll(),
                repaymentRepository.sumBetween(monthStart, today),
                upcoming.size(),
                overdue.size(),
                funnel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductPortfolioRow> portfolioByStatus(LoanStatus status) {
        return loanRepository.portfolioByStatus(status != null ? status : LoanStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmiDueResponse> upcomingEmis(int days) {
        LocalDate today = LocalDate.now();
        return emiScheduleRepository.findDueBetween(today, today.plusDays(days), UNPAID_STATUSES);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmiDueResponse> overdueEmis() {
        return emiScheduleRepository.findDueBefore(LocalDate.now(), UNPAID_STATUSES);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CollectionSummaryRow> collectionSummary(LocalDate from, LocalDate to) {
        LocalDate effectiveFrom = from != null ? from : LocalDate.now().minusDays(30);
        LocalDate effectiveTo = to != null ? to : LocalDate.now();
        return repaymentRepository.collectionSummary(effectiveFrom, effectiveTo);
    }
}