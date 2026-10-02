package com.apsfc.loanmanagement.service;

import com.apsfc.loanmanagement.dto.request.DisburseRequest;
import com.apsfc.loanmanagement.dto.response.EmiScheduleResponse;
import com.apsfc.loanmanagement.dto.response.LoanResponse;
import com.apsfc.loanmanagement.enums.LoanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface LoanService {

    LoanResponse disburse(Long applicationId, DisburseRequest request);

    LoanResponse getById(Long id);

    Page<LoanResponse> listMine(LoanStatus status, Pageable pageable);

    Page<LoanResponse> listAll(LoanStatus status, Long productId,
                               LocalDate dateFrom, LocalDate dateTo, Pageable pageable);

    List<EmiScheduleResponse> getEmiSchedule(Long loanId);
}