package com.apsfc.loanmanagement.service;

import com.apsfc.loanmanagement.dto.request.CreateLoanProductRequest;
import com.apsfc.loanmanagement.dto.request.UpdateLoanProductRequest;
import com.apsfc.loanmanagement.dto.response.LoanProductResponse;
import com.apsfc.loanmanagement.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LoanProductService {

    Page<LoanProductResponse> list(ProductStatus status, Pageable pageable);

    LoanProductResponse getById(Long id);

    LoanProductResponse create(CreateLoanProductRequest request);

    LoanProductResponse update(Long id, UpdateLoanProductRequest request);
}