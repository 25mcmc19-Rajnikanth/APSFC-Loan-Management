package com.apsfc.loanmanagement.service.impl;

import com.apsfc.loanmanagement.dto.request.DisburseRequest;
import com.apsfc.loanmanagement.dto.response.EmiScheduleResponse;
import com.apsfc.loanmanagement.dto.response.LoanResponse;
import com.apsfc.loanmanagement.entity.ApplicationHistory;
import com.apsfc.loanmanagement.entity.Customer;
import com.apsfc.loanmanagement.entity.EmiSchedule;
import com.apsfc.loanmanagement.entity.Loan;
import com.apsfc.loanmanagement.entity.LoanApplication;
import com.apsfc.loanmanagement.entity.User;
import com.apsfc.loanmanagement.enums.ApplicationStatus;
import com.apsfc.loanmanagement.enums.PaymentStatus;
import com.apsfc.loanmanagement.enums.Role;
import com.apsfc.loanmanagement.exception.BusinessViolationException;
import com.apsfc.loanmanagement.exception.ForbiddenOperationException;
import com.apsfc.loanmanagement.exception.ResourceNotFoundException;
import com.apsfc.loanmanagement.mapper.LoanMapper;
import com.apsfc.loanmanagement.repository.ApplicationHistoryRepository;
import com.apsfc.loanmanagement.repository.CustomerRepository;
import com.apsfc.loanmanagement.repository.EmiScheduleRepository;
import com.apsfc.loanmanagement.repository.LoanApplicationRepository;
import com.apsfc.loanmanagement.repository.LoanRepository;
import com.apsfc.loanmanagement.repository.LoanSpecifications;
import com.apsfc.loanmanagement.security.SecurityUtil;
import com.apsfc.loanmanagement.service.LoanService;
import com.apsfc.loanmanagement.util.AmortizationRow;
import com.apsfc.loanmanagement.util.AmortizationScheduleGenerator;
import com.apsfc.loanmanagement.util.ApplicationStateMachine;
import com.apsfc.loanmanagement.util.ReferenceNumberGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class LoanServiceImpl implements LoanService {

    private static final Logger log = LoggerFactory.getLogger(LoanServiceImpl.class);

    private final LoanRepository loanRepository;
    private final LoanApplicationRepository applicationRepository;
    private final ApplicationHistoryRepository historyRepository;
    private final EmiScheduleRepository emiScheduleRepository;
    private final CustomerRepository customerRepository;
    private final LoanMapper loanMapper;
    private final AmortizationScheduleGenerator amortizationGenerator;
    private final ApplicationStateMachine stateMachine;
    private final ReferenceNumberGenerator referenceNumberGenerator;

    public LoanServiceImpl(LoanRepository loanRepository,
                           LoanApplicationRepository applicationRepository,
                           ApplicationHistoryRepository historyRepository,
                           EmiScheduleRepository emiScheduleRepository,
                           CustomerRepository customerRepository,
                           LoanMapper loanMapper,
                           AmortizationScheduleGenerator amortizationGenerator,
                           ApplicationStateMachine stateMachine,
                           ReferenceNumberGenerator referenceNumberGenerator) {
        this.loanRepository = loanRepository;
        this.applicationRepository = applicationRepository;
        this.historyRepository = historyRepository;
        this.emiScheduleRepository = emiScheduleRepository;
        this.customerRepository = customerRepository;
        this.loanMapper = loanMapper;
        this.amortizationGenerator = amortizationGenerator;
        this.stateMachine = stateMachine;
        this.referenceNumberGenerator = referenceNumberGenerator;
    }

    /**
     * Atomic disbursement: creates the loan, generates the full EMI schedule,
     * and moves the application to DISBURSED - all in one transaction. If any
     * step fails nothing is persisted.
     */
    @Override
    @Transactional
    public LoanResponse disburse(Long applicationId, DisburseRequest request) {
        User me = SecurityUtil.currentUser();
        LoanApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Loan application", applicationId));

        if (application.getStatus() != ApplicationStatus.APPROVED) {
            throw new BusinessViolationException(
                    "Only APPROVED applications can be disbursed (current status: "
                            + application.getStatus() + ")");
        }
        if (loanRepository.findByApplicationId(applicationId).isPresent()) {
            throw new BusinessViolationException(
                    "Application " + application.getApplicationNumber() + " has already been disbursed");
        }

        BigDecimal principal = application.getLoanAmount();
        BigDecimal rate = application.getInterestRate();
        int tenure = application.getTenureMonths();
        LocalDate disbursementDate = request.disbursementDate() != null
                ? request.disbursementDate() : LocalDate.now();
        BigDecimal disbursedAmount = request.disbursedAmount() != null
                ? request.disbursedAmount() : principal;

        List<AmortizationRow> rows = amortizationGenerator.generate(
                principal, rate, tenure, application.getMonthlyEmi(), disbursementDate);
        BigDecimal totalInterest = rows.stream()
                .map(AmortizationRow::interestComponent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAmount = rows.stream()
                .map(AmortizationRow::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Loan loan = Loan.builder()
                .loanNumber(referenceNumberGenerator.nextLoanNumber())
                .application(application)
                .customer(application.getCustomer())
                .product(application.getProduct())
                .principalAmount(principal)
                .interestRate(rate)
                .tenureMonths(tenure)
                .monthlyEmi(application.getMonthlyEmi())
                .totalInterest(totalInterest)
                .totalAmount(totalAmount)
                .disbursedAmount(disbursedAmount)
                .disbursementDate(disbursementDate)
                .amountPaid(BigDecimal.ZERO)
                .outstandingBalance(totalAmount)
                .status(com.apsfc.loanmanagement.enums.LoanStatus.ACTIVE)
                .disbursedBy(me)
                .build();
        loan = loanRepository.save(loan);
        Loan saved = loan;

        List<EmiSchedule> schedule = rows.stream()
                .map(r -> EmiSchedule.builder()
                        .loan(saved)
                        .emiNumber(r.emiNumber())
                        .dueDate(r.dueDate())
                        .principalComponent(r.principalComponent())
                        .interestComponent(r.interestComponent())
                        .totalAmount(r.totalAmount())
                        .cumulativePaid(BigDecimal.ZERO)
                        .paymentStatus(PaymentStatus.PENDING)
                        .build())
                .toList();
        emiScheduleRepository.saveAll(schedule);

        applyDisbursementTransition(application, loan.getLoanNumber(), me);
        log.info("Loan {} disbursed for application {} by manager {}",
                loan.getLoanNumber(), application.getApplicationNumber(), me.getUsername());
        return loanMapper.toResponse(loan);
    }

    @Override
    @Transactional(readOnly = true)
    public LoanResponse getById(Long id) {
        return loanMapper.toResponse(findLoanWithAccess(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LoanResponse> listMine(com.apsfc.loanmanagement.enums.LoanStatus status, Pageable pageable) {
        User me = SecurityUtil.currentUser();
        Customer customer = currentCustomer(me);

        Specification<Loan> spec = Specification
                .where(LoanSpecifications.belongsToCustomer(customer.getId()));
        if (status != null) {
            spec = spec.and(LoanSpecifications.hasStatus(status));
        }
        return loanRepository.findAll(spec, pageable).map(loanMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LoanResponse> listAll(com.apsfc.loanmanagement.enums.LoanStatus status, Long productId,
                                      LocalDate dateFrom, LocalDate dateTo, Pageable pageable) {
        Specification<Loan> spec = Specification.where(null);
        if (status != null) {
            spec = spec.and(LoanSpecifications.hasStatus(status));
        }
        if (productId != null) {
            spec = spec.and(LoanSpecifications.hasProduct(productId));
        }
        LocalDateTime from = dateFrom != null ? dateFrom.atStartOfDay() : null;
        LocalDateTime to = dateTo != null ? dateTo.atTime(LocalTime.MAX) : null;
        spec = spec.and(LoanSpecifications.disbursedBetween(from, to));
        return loanRepository.findAll(spec, pageable).map(loanMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmiScheduleResponse> getEmiSchedule(Long loanId) {
        Loan loan = findLoanWithAccess(loanId);
        return loanMapper.toEmiResponses(
                emiScheduleRepository.findByLoanIdOrderByEmiNumberAsc(loan.getId()));
    }

    private void applyDisbursementTransition(LoanApplication application, String loanNumber, User actor) {
        ApplicationStatus from = application.getStatus();
        stateMachine.enforce(from, ApplicationStatus.DISBURSED);
        application.setStatus(ApplicationStatus.DISBURSED);
        applicationRepository.save(application);
        historyRepository.save(ApplicationHistory.builder()
                .application(application)
                .fromStatus(from.name())
                .toStatus(ApplicationStatus.DISBURSED.name())
                .action("DISBURSED")
                .comment("Disbursed as " + loanNumber)
                .performedBy(actor)
                .performedAt(LocalDateTime.now())
                .build());
    }

    private Customer currentCustomer(User user) {
        return customerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessViolationException(
                        "Customer profile not found for user " + user.getUsername()));
    }

    private Loan findLoan(Long id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Loan", id));
    }

    private Loan findLoanWithAccess(Long id) {
        Loan loan = findLoan(id);
        User me = SecurityUtil.currentUser();
        if (me.getRole() == Role.CUSTOMER) {
            Customer customer = currentCustomer(me);
            if (!loan.getCustomer().getId().equals(customer.getId())) {
                throw new ForbiddenOperationException("You can only access your own loans");
            }
        }
        return loan;
    }
}