package com.apsfc.loanmanagement.service;

import com.apsfc.loanmanagement.dto.request.CreateLoanApplicationRequest;
import com.apsfc.loanmanagement.dto.request.DecisionRequest;
import com.apsfc.loanmanagement.dto.request.RecommendRequest;
import com.apsfc.loanmanagement.dto.request.UpdateLoanApplicationRequest;
import com.apsfc.loanmanagement.dto.response.ApplicationHistoryResponse;
import com.apsfc.loanmanagement.dto.response.LoanApplicationResponse;
import com.apsfc.loanmanagement.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface LoanApplicationService {

    LoanApplicationResponse create(CreateLoanApplicationRequest request);

    LoanApplicationResponse updateDraft(Long id, UpdateLoanApplicationRequest request);

    LoanApplicationResponse submit(Long id);

    LoanApplicationResponse cancel(Long id);

    LoanApplicationResponse getById(Long id);

    Page<LoanApplicationResponse> listMine(ApplicationStatus status, Pageable pageable);

    Page<LoanApplicationResponse> listAll(ApplicationStatus status, Long productId,
                                          LocalDate dateFrom, LocalDate dateTo, Pageable pageable);

    LoanApplicationResponse startReview(Long id);

    LoanApplicationResponse recommend(Long id, RecommendRequest request);

    LoanApplicationResponse approve(Long id, DecisionRequest request);

    LoanApplicationResponse reject(Long id, DecisionRequest request);

    List<ApplicationHistoryResponse> getHistory(Long id);
}