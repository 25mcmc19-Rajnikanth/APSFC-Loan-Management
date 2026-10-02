package com.apsfc.loanmanagement.service.impl;

import com.apsfc.loanmanagement.dto.response.CustomerLoanAggregate;
import com.apsfc.loanmanagement.dto.response.CustomerResponse;
import com.apsfc.loanmanagement.entity.Customer;
import com.apsfc.loanmanagement.enums.LoanStatus;
import com.apsfc.loanmanagement.enums.UserStatus;
import com.apsfc.loanmanagement.exception.BusinessViolationException;
import com.apsfc.loanmanagement.exception.ResourceNotFoundException;
import com.apsfc.loanmanagement.mapper.CustomerMapper;
import com.apsfc.loanmanagement.repository.CustomerRepository;
import com.apsfc.loanmanagement.repository.CustomerSpecifications;
import com.apsfc.loanmanagement.repository.LoanRepository;
import com.apsfc.loanmanagement.service.CustomerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CustomerServiceImpl implements CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerServiceImpl.class);

    private final CustomerRepository customerRepository;
    private final LoanRepository loanRepository;
    private final CustomerMapper customerMapper;

    public CustomerServiceImpl(CustomerRepository customerRepository,
                               LoanRepository loanRepository,
                               CustomerMapper customerMapper) {
        this.customerRepository = customerRepository;
        this.loanRepository = loanRepository;
        this.customerMapper = customerMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerResponse> list(UserStatus status, String search, Boolean kycVerified, Pageable pageable) {
        Map<Long, CustomerLoanAggregate> aggregates = loanRepository
                .loanAggregatesByStatus(LoanStatus.ACTIVE)
                .stream()
                .collect(Collectors.toMap(CustomerLoanAggregate::customerId, a -> a));

        Specification<Customer> spec = Specification.where(CustomerSpecifications.search(search));
        if (status != null) {
            spec = spec.and(CustomerSpecifications.hasStatus(status));
        }
        if (kycVerified != null) {
            spec = spec.and(CustomerSpecifications.kycStatus(kycVerified));
        }
        return customerRepository.findAll(spec, pageable)
                .map(c -> toResponseWithAggregate(c, aggregates.get(c.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getById(Long id) {
        Customer customer = findCustomer(id);
        CustomerLoanAggregate aggregate = loanRepository.loanAggregatesByStatus(LoanStatus.ACTIVE)
                .stream()
                .filter(a -> a.customerId().equals(customer.getId()))
                .findFirst().orElse(null);
        return toResponseWithAggregate(customer, aggregate);
    }

    @Override
    @Transactional
    public CustomerResponse updateStatus(Long id, UserStatus status) {
        validateToggleableStatus(status);
        Customer customer = findCustomer(id);
        customer.getUser().setStatus(status);
        customerRepository.save(customer);
        log.info("Customer {} status set to {} by {}", customer.getUser().getUsername(), status,
                com.apsfc.loanmanagement.security.SecurityUtil.currentUser().getUsername());
        return customerMapper.toResponse(customer, null, null);
    }

    @Override
    @Transactional
    public CustomerResponse verifyKyc(Long id) {
        Customer customer = findCustomer(id);
        customer.setKycVerified(true);
        customerRepository.save(customer);
        log.info("KYC verified for customer {}", customer.getUser().getUsername());
        return customerMapper.toResponse(customer, null, null);
    }

    private CustomerResponse toResponseWithAggregate(Customer customer,
                                                     CustomerLoanAggregate aggregate) {
        Long activeLoans = aggregate != null ? aggregate.activeLoanCount() : 0L;
        BigDecimal outstanding = aggregate != null ? aggregate.outstandingBalance() : BigDecimal.ZERO;
        return customerMapper.toResponse(customer, activeLoans, outstanding);
    }

    private void validateToggleableStatus(UserStatus status) {
        if (status == UserStatus.LOCKED) {
            throw new BusinessViolationException("LOCKED cannot be set via admin; use ACTIVE or INACTIVE");
        }
    }

    private Customer findCustomer(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Customer", id));
    }
}