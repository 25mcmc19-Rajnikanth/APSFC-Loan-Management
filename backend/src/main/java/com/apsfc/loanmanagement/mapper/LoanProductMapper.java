package com.apsfc.loanmanagement.mapper;

import com.apsfc.loanmanagement.dto.response.LoanProductResponse;
import com.apsfc.loanmanagement.entity.LoanProduct;
import org.springframework.stereotype.Component;

@Component
public class LoanProductMapper {

    public LoanProductResponse toResponse(LoanProduct product) {
        return new LoanProductResponse(
                product.getId(),
                product.getName(),
                product.getCode(),
                product.getDescription(),
                product.getMinAmount(),
                product.getMaxAmount(),
                product.getBaseInterestRate(),
                product.getMinTenureMonths(),
                product.getMaxTenureMonths(),
                product.getEligibilityCriteria(),
                product.getStatus(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}