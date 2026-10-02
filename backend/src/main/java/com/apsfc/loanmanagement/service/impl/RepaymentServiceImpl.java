package com.apsfc.loanmanagement.service.impl;

import com.apsfc.loanmanagement.dto.request.CreateRepaymentRequest;
import com.apsfc.loanmanagement.dto.response.RepaymentResponse;
import com.apsfc.loanmanagement.entity.Customer;
import com.apsfc.loanmanagement.entity.EmiSchedule;
import com.apsfc.loanmanagement.entity.Loan;
import com.apsfc.loanmanagement.entity.Repayment;
import com.apsfc.loanmanagement.entity.User;
import com.apsfc.loanmanagement.enums.LoanStatus;
import com.apsfc.loanmanagement.enums.PaymentStatus;
import com.apsfc.loanmanagement.enums.Role;
import com.apsfc.loanmanagement.exception.BusinessViolationException;
import com.apsfc.loanmanagement.exception.ForbiddenOperationException;
import com.apsfc.loanmanagement.exception.InvalidRepaymentException;
import com.apsfc.loanmanagement.exception.ResourceNotFoundException;
import com.apsfc.loanmanagement.mapper.RepaymentMapper;
import com.apsfc.loanmanagement.repository.CustomerRepository;
import com.apsfc.loanmanagement.repository.EmiScheduleRepository;
import com.apsfc.loanmanagement.repository.LoanRepository;
import com.apsfc.loanmanagement.repository.RepaymentRepository;
import com.apsfc.loanmanagement.repository.RepaymentSpecifications;
import com.apsfc.loanmanagement.security.SecurityUtil;
import com.apsfc.loanmanagement.service.RepaymentService;
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
import java.util.Set;

@Service
public class RepaymentServiceImpl implements RepaymentService {

    private static final Logger log = LoggerFactory.getLogger(RepaymentServiceImpl.class);

    private final RepaymentRepository repaymentRepository;
    private final LoanRepository loanRepository;
    private final EmiScheduleRepository emiScheduleRepository;
    private final CustomerRepository customerRepository;
    private final RepaymentMapper repaymentMapper;

    public RepaymentServiceImpl(RepaymentRepository repaymentRepository,
                                LoanRepository loanRepository,
                                EmiScheduleRepository emiScheduleRepository,
                                CustomerRepository customerRepository,
                                RepaymentMapper repaymentMapper) {
        this.repaymentRepository = repaymentRepository;
        this.loanRepository = loanRepository;
        this.emiScheduleRepository = emiScheduleRepository;
        this.customerRepository = customerRepository;
        this.repaymentMapper = repaymentMapper;
    }

    /**
     * Records a payment against the next unpaid installment of the loan. The
     * amount must not exceed that installment's remaining due (installments
     * are settled strictly in order, guaranteeing reconciliation).
     */
    @Override
    @Transactional
    public RepaymentResponse record(Long loanId, CreateRepaymentRequest request) {
        User me = SecurityUtil.currentUser();
        Loan loan = findLoanWithAccess(loanId);

        if (loan.getStatus() == LoanStatus.CLOSED) {
            throw new InvalidRepaymentException("Loan " + loan.getLoanNumber() + " is already closed");
        }

        LocalDate paymentDate = request.paymentDate() != null ? request.paymentDate() : LocalDate.now();

        List<EmiSchedule> dueEmis = emiScheduleRepository
                .findByLoanIdAndPaymentStatusInOrderByEmiNumberAsc(loanId,
                        Set.of(PaymentStatus.PENDING, PaymentStatus.PARTIALLY_PAID));
        if (dueEmis.isEmpty()) {
            throw new InvalidRepaymentException("Loan " + loan.getLoanNumber() + " has no outstanding installments");
        }
        EmiSchedule currentEmi = dueEmis.get(0);

        BigDecimal remainingDue = currentEmi.getTotalAmount().subtract(currentEmi.getCumulativePaid());
        if (request.amount().compareTo(remainingDue) > 0) {
            throw new InvalidRepaymentException(
                    "Payment amount " + request.amount() + " exceeds the outstanding installment of "
                            + remainingDue + " on EMI #" + currentEmi.getEmiNumber()
                            + ". Installments are settled in order.");
        }

        BigDecimal newCumulative = currentEmi.getCumulativePaid().add(request.amount());
        currentEmi.setCumulativePaid(newCumulative);
        currentEmi.setPaidDate(paymentDate);
        currentEmi.setPaymentStatus(newCumulative.compareTo(currentEmi.getTotalAmount()) >= 0
                ? PaymentStatus.PAID : PaymentStatus.PARTIALLY_PAID);
        emiScheduleRepository.save(currentEmi);

        BigDecimal newAmountPaid = loan.getAmountPaid().add(request.amount());
        loan.setAmountPaid(newAmountPaid);
        loan.setOutstandingBalance(loan.getTotalAmount().subtract(newAmountPaid));
        if (loan.getOutstandingBalance().compareTo(BigDecimal.ZERO) == 0) {
            loan.setStatus(LoanStatus.CLOSED);
            loan.setClosedAt(LocalDateTime.now());
        }
        loanRepository.save(loan);

        Repayment repayment = Repayment.builder()
                .loan(loan)
                .emiAllocation(currentEmi)
                .amount(request.amount())
                .paymentMode(request.paymentMode())
                .transactionRef(request.transactionRef())
                .paymentDate(paymentDate)
                .createdBy(me)
                .remarks(request.remarks())
                .build();
        repayment = repaymentRepository.save(repayment);

        log.info("Repayment of {} recorded on loan {} (EMI #{}, now {}) by {}",
                request.amount(), loan.getLoanNumber(), currentEmi.getEmiNumber(),
                currentEmi.getPaymentStatus(), me.getUsername());
        return repaymentMapper.toResponse(repayment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RepaymentResponse> listByLoan(Long loanId, Pageable pageable) {
        Loan loan = findLoanWithAccess(loanId);
        return repaymentRepository.findAll(RepaymentSpecifications.onLoan(loan.getId()), pageable)
                .map(repaymentMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RepaymentResponse> listAll(Long loanId, LocalDate dateFrom, LocalDate dateTo, Pageable pageable) {
        Specification<Repayment> spec = Specification.where(null);
        if (loanId != null) {
            spec = spec.and(RepaymentSpecifications.onLoan(loanId));
        }
        spec = spec.and(RepaymentSpecifications.paidBetween(
                dateFrom != null ? dateFrom.atStartOfDay() : null,
                dateTo != null ? dateTo.atTime(LocalTime.MAX) : null));
        return repaymentRepository.findAll(spec, pageable).map(repaymentMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RepaymentResponse> listMine(LocalDate dateFrom, LocalDate dateTo, Pageable pageable) {
        User me = SecurityUtil.currentUser();
        Customer customer = currentCustomer(me);

        Specification<Repayment> spec = Specification
                .where(RepaymentSpecifications.ofCustomer(customer.getId()))
                .and(RepaymentSpecifications.paidBetween(
                        dateFrom != null ? dateFrom.atStartOfDay() : null,
                        dateTo != null ? dateTo.atTime(LocalTime.MAX) : null));
        return repaymentRepository.findAll(spec, pageable).map(repaymentMapper::toResponse);
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

    /**
     * Customers may only touch their own loans; staff may record collections
     * and view repayment history on any loan.
     */
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