package com.apsfc.loanmanagement.service.impl;

import com.apsfc.loanmanagement.dto.request.CreateLoanApplicationRequest;
import com.apsfc.loanmanagement.dto.request.DecisionRequest;
import com.apsfc.loanmanagement.dto.request.RecommendRequest;
import com.apsfc.loanmanagement.dto.request.UpdateLoanApplicationRequest;
import com.apsfc.loanmanagement.dto.response.ApplicationHistoryResponse;
import com.apsfc.loanmanagement.dto.response.LoanApplicationResponse;
import com.apsfc.loanmanagement.entity.ApplicationHistory;
import com.apsfc.loanmanagement.entity.Customer;
import com.apsfc.loanmanagement.entity.LoanApplication;
import com.apsfc.loanmanagement.entity.LoanProduct;
import com.apsfc.loanmanagement.entity.User;
import com.apsfc.loanmanagement.enums.ApplicationStatus;
import com.apsfc.loanmanagement.enums.ProductStatus;
import com.apsfc.loanmanagement.enums.Role;
import com.apsfc.loanmanagement.exception.BusinessViolationException;
import com.apsfc.loanmanagement.exception.ForbiddenOperationException;
import com.apsfc.loanmanagement.exception.ResourceNotFoundException;
import com.apsfc.loanmanagement.mapper.LoanApplicationMapper;
import com.apsfc.loanmanagement.repository.ApplicationHistoryRepository;
import com.apsfc.loanmanagement.repository.CustomerRepository;
import com.apsfc.loanmanagement.repository.LoanApplicationRepository;
import com.apsfc.loanmanagement.repository.LoanApplicationSpecifications;
import com.apsfc.loanmanagement.repository.LoanProductRepository;
import com.apsfc.loanmanagement.security.SecurityUtil;
import com.apsfc.loanmanagement.service.LoanApplicationService;
import com.apsfc.loanmanagement.util.ApplicationStateMachine;
import com.apsfc.loanmanagement.util.EmiCalculator;
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
public class LoanApplicationServiceImpl implements LoanApplicationService {

    private static final Logger log = LoggerFactory.getLogger(LoanApplicationServiceImpl.class);

    private final LoanApplicationRepository applicationRepository;
    private final ApplicationHistoryRepository historyRepository;
    private final CustomerRepository customerRepository;
    private final LoanProductRepository productRepository;
    private final LoanApplicationMapper applicationMapper;
    private final ApplicationStateMachine stateMachine;
    private final EmiCalculator emiCalculator;
    private final ReferenceNumberGenerator referenceNumberGenerator;

    public LoanApplicationServiceImpl(LoanApplicationRepository applicationRepository,
                                      ApplicationHistoryRepository historyRepository,
                                      CustomerRepository customerRepository,
                                      LoanProductRepository productRepository,
                                      LoanApplicationMapper applicationMapper,
                                      ApplicationStateMachine stateMachine,
                                      EmiCalculator emiCalculator,
                                      ReferenceNumberGenerator referenceNumberGenerator) {
        this.applicationRepository = applicationRepository;
        this.historyRepository = historyRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.applicationMapper = applicationMapper;
        this.stateMachine = stateMachine;
        this.emiCalculator = emiCalculator;
        this.referenceNumberGenerator = referenceNumberGenerator;
    }

    @Override
    @Transactional
    public LoanApplicationResponse create(CreateLoanApplicationRequest request) {
        User me = SecurityUtil.currentUser();
        Customer customer = currentCustomer(me);
        LoanProduct product = findActiveProduct(request.productId());

        validateAgainstProduct(product, request.loanAmount(), request.tenureMonths());

        LoanApplication application = LoanApplication.builder()
                .applicationNumber(referenceNumberGenerator.nextApplicationNumber())
                .customer(customer)
                .product(product)
                .loanAmount(request.loanAmount())
                .tenureMonths(request.tenureMonths())
                .interestRate(product.getBaseInterestRate())
                .monthlyEmi(emiCalculator.computeEmi(
                        request.loanAmount(), product.getBaseInterestRate(), request.tenureMonths()))
                .purpose(request.purpose())
                .status(ApplicationStatus.DRAFT)
                .build();
        application = applicationRepository.save(application);

        recordHistory(application, null, ApplicationStatus.DRAFT, "CREATED", null, me);
        log.info("Loan application created: {} by customer {}", application.getApplicationNumber(),
                customer.getUser().getUsername());
        return applicationMapper.toResponse(application);
    }

    @Override
    @Transactional
    public LoanApplicationResponse updateDraft(Long id, UpdateLoanApplicationRequest request) {
        User me = SecurityUtil.currentUser();
        LoanApplication application = findOwnedApplication(id);

        if (application.getStatus() != ApplicationStatus.DRAFT) {
            throw new BusinessViolationException(
                    "Only DRAFT applications can be edited (current status: " + application.getStatus() + ")");
        }

        LoanProduct product = application.getProduct();
        boolean productChanged = request.productId() != null
                && !request.productId().equals(product.getId());
        if (productChanged) {
            product = findActiveProduct(request.productId());
            application.setProduct(product);
        }

        boolean financialChanged = productChanged
                || request.loanAmount() != null
                || request.tenureMonths() != null;

        if (financialChanged) {
            BigDecimal amount = request.loanAmount() != null ? request.loanAmount() : application.getLoanAmount();
            Integer tenure = request.tenureMonths() != null ? request.tenureMonths() : application.getTenureMonths();
            validateAgainstProduct(product, amount, tenure);
            application.setLoanAmount(amount);
            application.setTenureMonths(tenure);
            application.setInterestRate(product.getBaseInterestRate());
            application.setMonthlyEmi(emiCalculator.computeEmi(amount, product.getBaseInterestRate(), tenure));
        }

        if (request.purpose() != null) {
            application.setPurpose(request.purpose());
        }
        return applicationMapper.toResponse(applicationRepository.save(application));
    }

    @Override
    @Transactional
    public LoanApplicationResponse submit(Long id) {
        User me = SecurityUtil.currentUser();
        LoanApplication application = findOwnedApplication(id);
        application.setSubmittedBy(me);
        application.setSubmittedAt(LocalDateTime.now());
        applyTransition(application, ApplicationStatus.SUBMITTED, "SUBMITTED", me, null);
        log.info("Application {} submitted by customer {}", application.getApplicationNumber(),
                me.getUsername());
        return applicationMapper.toResponse(application);
    }

    @Override
    @Transactional
    public LoanApplicationResponse cancel(Long id) {
        User me = SecurityUtil.currentUser();
        LoanApplication application = findOwnedApplication(id);
        applyTransition(application, ApplicationStatus.CANCELLED, "CANCELLED", me,
                "Cancelled by customer");
        return applicationMapper.toResponse(application);
    }

    @Override
    @Transactional(readOnly = true)
    public LoanApplicationResponse getById(Long id) {
        return applicationMapper.toResponse(findApplicationWithAccess(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LoanApplicationResponse> listMine(ApplicationStatus status, Pageable pageable) {
        User me = SecurityUtil.currentUser();
        Customer customer = currentCustomer(me);

        Specification<LoanApplication> spec = Specification
                .where(LoanApplicationSpecifications.belongsToCustomer(customer.getId()));
        if (status != null) {
            spec = spec.and(LoanApplicationSpecifications.hasStatus(status));
        }
        return applicationRepository.findAll(spec, pageable).map(applicationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LoanApplicationResponse> listAll(ApplicationStatus status, Long productId,
                                                 LocalDate dateFrom, LocalDate dateTo, Pageable pageable) {
        Specification<LoanApplication> spec = Specification.where(null);
        if (status != null) {
            spec = spec.and(LoanApplicationSpecifications.hasStatus(status));
        }
        if (productId != null) {
            spec = spec.and(LoanApplicationSpecifications.hasProduct(productId));
        }
        LocalDateTime from = dateFrom != null ? dateFrom.atStartOfDay() : null;
        LocalDateTime to = dateTo != null ? dateTo.atTime(LocalTime.MAX) : null;
        spec = spec.and(LoanApplicationSpecifications.createdBetween(from, to));
        return applicationRepository.findAll(spec, pageable).map(applicationMapper::toResponse);
    }

    @Override
    @Transactional
    public LoanApplicationResponse startReview(Long id) {
        User me = SecurityUtil.currentUser();
        LoanApplication application = findApplication(id);
        if (application.getStatus() != ApplicationStatus.SUBMITTED) {
            throw new BusinessViolationException(
                    "Only SUBMITTED applications can start review (current status: " + application.getStatus() + ")");
        }
        application.setReviewedBy(me);
        application.setReviewedAt(LocalDateTime.now());
        applyTransition(application, ApplicationStatus.UNDER_REVIEW, "REVIEW_STARTED", me, null);
        log.info("Review started for application {} by {}", application.getApplicationNumber(), me.getUsername());
        return applicationMapper.toResponse(application);
    }

    @Override
    @Transactional
    public LoanApplicationResponse recommend(Long id, RecommendRequest request) {
        User me = SecurityUtil.currentUser();
        LoanApplication application = findApplication(id);
        if (application.getStatus() != ApplicationStatus.UNDER_REVIEW) {
            throw new BusinessViolationException(
                    "Recommendation is allowed only while UNDER_REVIEW (current status: "
                            + application.getStatus() + ")");
        }
        application.setReviewComment(request.comment());
        applicationRepository.save(application);
        recordHistory(application, application.getStatus(), application.getStatus(),
                "RECOMMENDED_" + request.recommendation(), request.comment(), me);
        log.info("Officer {} recommended {} for application {}",
                me.getUsername(), request.recommendation(), application.getApplicationNumber());
        return applicationMapper.toResponse(application);
    }

    @Override
    @Transactional
    public LoanApplicationResponse approve(Long id, DecisionRequest request) {
        User me = SecurityUtil.currentUser();
        LoanApplication application = findApplication(id);
        if (application.getStatus() != ApplicationStatus.UNDER_REVIEW) {
            throw new BusinessViolationException(
                    "Only applications UNDER_REVIEW can be approved (current status: "
                            + application.getStatus() + ")");
        }
        application.setApprovedBy(me);
        application.setDecisionComment(request.comment());
        application.setDecidedAt(LocalDateTime.now());
        applyTransition(application, ApplicationStatus.APPROVED, "APPROVED", me, request.comment());
        log.info("Application {} approved by manager {}", application.getApplicationNumber(), me.getUsername());
        return applicationMapper.toResponse(application);
    }

    @Override
    @Transactional
    public LoanApplicationResponse reject(Long id, DecisionRequest request) {
        User me = SecurityUtil.currentUser();
        LoanApplication application = findApplication(id);
        ApplicationStatus current = application.getStatus();
        if (current != ApplicationStatus.SUBMITTED && current != ApplicationStatus.UNDER_REVIEW) {
            throw new BusinessViolationException(
                    "Only SUBMITTED or UNDER_REVIEW applications can be rejected (current status: " + current + ")");
        }
        application.setApprovedBy(me);
        application.setDecisionComment(request.comment());
        application.setDecidedAt(LocalDateTime.now());
        applyTransition(application, ApplicationStatus.REJECTED, "REJECTED", me, request.comment());
        log.info("Application {} rejected by manager {}", application.getApplicationNumber(), me.getUsername());
        return applicationMapper.toResponse(application);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationHistoryResponse> getHistory(Long id) {
        LoanApplication application = findApplicationWithAccess(id);
        return applicationMapper.toHistoryResponses(
                historyRepository.findByApplicationIdOrderByPerformedAtAsc(application.getId()));
    }

    /**
     * Audited, state-machine-enforced transition. This is the only path that
     * mutates application status.
     */
    private void applyTransition(LoanApplication application, ApplicationStatus to,
                                 String action, User actor, String comment) {
        ApplicationStatus from = application.getStatus();
        stateMachine.enforce(from, to);
        application.setStatus(to);
        applicationRepository.save(application);
        recordHistory(application, from, to, action, comment, actor);
    }

    private void recordHistory(LoanApplication application, ApplicationStatus from, ApplicationStatus to,
                               String action, String comment, User actor) {
        historyRepository.save(ApplicationHistory.builder()
                .application(application)
                .fromStatus(from != null ? from.name() : null)
                .toStatus(to.name())
                .action(action)
                .comment(comment)
                .performedBy(actor)
                .performedAt(LocalDateTime.now())
                .build());
    }

    private void validateAgainstProduct(LoanProduct product, BigDecimal amount, Integer tenure) {
        if (amount.compareTo(product.getMinAmount()) < 0 || amount.compareTo(product.getMaxAmount()) > 0) {
            throw new BusinessViolationException("Loan amount must be between "
                    + product.getMinAmount() + " and " + product.getMaxAmount()
                    + " for product " + product.getName());
        }
        if (tenure < product.getMinTenureMonths() || tenure > product.getMaxTenureMonths()) {
            throw new BusinessViolationException("Tenure must be between "
                    + product.getMinTenureMonths() + " and " + product.getMaxTenureMonths()
                    + " months for product " + product.getName());
        }
    }

    private LoanProduct findActiveProduct(Long productId) {
        LoanProduct product = productRepository.findById(productId)
                .orElseThrow(() -> ResourceNotFoundException.of("Loan product", productId));
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new BusinessViolationException("Loan product is not active: " + product.getName());
        }
        return product;
    }

    private Customer currentCustomer(User user) {
        return customerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessViolationException(
                        "Customer profile not found for user " + user.getUsername()
                                + ". Please complete your profile."));
    }

    private LoanApplication findApplication(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Loan application", id));
    }

    private LoanApplication findOwnedApplication(Long id) {
        LoanApplication application = findApplication(id);
        User me = SecurityUtil.currentUser();
        Customer customer = currentCustomer(me);
        if (!application.getCustomer().getId().equals(customer.getId())) {
            throw new ForbiddenOperationException("You can only access your own loan applications");
        }
        return application;
    }

    /**
     * Ownership rule for read endpoints: customers see only their own
     * applications; staff see any.
     */
    private LoanApplication findApplicationWithAccess(Long id) {
        LoanApplication application = findApplication(id);
        User me = SecurityUtil.currentUser();
        if (me.getRole() == Role.CUSTOMER) {
            Customer customer = currentCustomer(me);
            if (!application.getCustomer().getId().equals(customer.getId())) {
                throw new ForbiddenOperationException("You can only access your own loan applications");
            }
        }
        return application;
    }
}