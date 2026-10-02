package com.apsfc.loanmanagement.service.impl;

import com.apsfc.loanmanagement.dto.request.CreateLoanProductRequest;
import com.apsfc.loanmanagement.dto.request.UpdateLoanProductRequest;
import com.apsfc.loanmanagement.dto.response.LoanProductResponse;
import com.apsfc.loanmanagement.entity.LoanProduct;
import com.apsfc.loanmanagement.enums.ProductStatus;
import com.apsfc.loanmanagement.exception.BusinessViolationException;
import com.apsfc.loanmanagement.exception.DuplicateResourceException;
import com.apsfc.loanmanagement.exception.ResourceNotFoundException;
import com.apsfc.loanmanagement.mapper.LoanProductMapper;
import com.apsfc.loanmanagement.repository.LoanProductRepository;
import com.apsfc.loanmanagement.service.LoanProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Service
public class LoanProductServiceImpl implements LoanProductService {

    private final LoanProductRepository productRepository;
    private final LoanProductMapper productMapper;

    public LoanProductServiceImpl(LoanProductRepository productRepository, LoanProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LoanProductResponse> list(ProductStatus status, Pageable pageable) {
        Page<LoanProduct> products = (status == null)
                ? productRepository.findAll(pageable)
                : productRepository.findByStatus(status, pageable);
        return products.map(productMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public LoanProductResponse getById(Long id) {
        return productMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public LoanProductResponse create(CreateLoanProductRequest request) {
        if (productRepository.existsByCode(request.code())) {
            throw new DuplicateResourceException("A loan product with code " + request.code() + " already exists");
        }
        validateRanges(request.minAmount(), request.maxAmount(),
                request.minTenureMonths(), request.maxTenureMonths());

        LoanProduct product = LoanProduct.builder()
                .name(request.name())
                .code(request.code())
                .description(request.description())
                .minAmount(request.minAmount())
                .maxAmount(request.maxAmount())
                .baseInterestRate(request.baseInterestRate())
                .minTenureMonths(request.minTenureMonths())
                .maxTenureMonths(request.maxTenureMonths())
                .eligibilityCriteria(request.eligibilityCriteria())
                .status(ProductStatus.ACTIVE)
                .build();
        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public LoanProductResponse update(Long id, UpdateLoanProductRequest request) {
        LoanProduct product = findOrThrow(id);

        if (request.name() != null) {
            product.setName(request.name());
        }
        if (StringUtils.hasText(request.code())) {
            productRepository.findByCode(request.code())
                    .filter(existing -> !existing.getId().equals(product.getId()))
                    .ifPresent(existing -> {
                        throw new DuplicateResourceException(
                                "A loan product with code " + request.code() + " already exists");
                    });
            product.setCode(request.code());
        }
        if (request.description() != null) {
            product.setDescription(request.description());
        }
        if (request.minAmount() != null) {
            product.setMinAmount(request.minAmount());
        }
        if (request.maxAmount() != null) {
            product.setMaxAmount(request.maxAmount());
        }
        if (request.baseInterestRate() != null) {
            product.setBaseInterestRate(request.baseInterestRate());
        }
        if (request.minTenureMonths() != null) {
            product.setMinTenureMonths(request.minTenureMonths());
        }
        if (request.maxTenureMonths() != null) {
            product.setMaxTenureMonths(request.maxTenureMonths());
        }
        if (request.eligibilityCriteria() != null) {
            product.setEligibilityCriteria(request.eligibilityCriteria());
        }
        if (request.status() != null) {
            product.setStatus(request.status());
        }

        validateRanges(product.getMinAmount(), product.getMaxAmount(),
                product.getMinTenureMonths(), product.getMaxTenureMonths());

        return productMapper.toResponse(productRepository.save(product));
    }

    private LoanProduct findOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Loan product", id));
    }

    /**
     * Cross-field business rules that cannot be expressed as single-field bean
     * validations. Centralised here so create and update enforce identical rules.
     */
    private void validateRanges(BigDecimal minAmount, BigDecimal maxAmount,
                                Integer minTenure, Integer maxTenure) {
        if (minAmount == null || maxAmount == null || minTenure == null || maxTenure == null) {
            return;
        }
        if (maxAmount.compareTo(minAmount) < 0) {
            throw new BusinessViolationException("Maximum amount cannot be less than minimum amount");
        }
        if (maxTenure < minTenure) {
            throw new BusinessViolationException("Maximum tenure cannot be less than minimum tenure");
        }
    }
}