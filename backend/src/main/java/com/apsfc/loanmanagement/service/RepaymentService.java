package com.apsfc.loanmanagement.service;

import com.apsfc.loanmanagement.dto.request.CreateRepaymentRequest;
import com.apsfc.loanmanagement.dto.response.RepaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface RepaymentService {

    RepaymentResponse record(Long loanId, CreateRepaymentRequest request);

    Page<RepaymentResponse> listByLoan(Long loanId, Pageable pageable);

    Page<RepaymentResponse> listAll(Long loanId, LocalDate dateFrom, LocalDate dateTo, Pageable pageable);

    Page<RepaymentResponse> listMine(LocalDate dateFrom, LocalDate dateTo, Pageable pageable);
}