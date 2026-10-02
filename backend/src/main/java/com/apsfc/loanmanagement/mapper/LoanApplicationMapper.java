package com.apsfc.loanmanagement.mapper;

import com.apsfc.loanmanagement.dto.response.ApplicationHistoryResponse;
import com.apsfc.loanmanagement.dto.response.LoanApplicationResponse;
import com.apsfc.loanmanagement.entity.ApplicationHistory;
import com.apsfc.loanmanagement.entity.LoanApplication;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LoanApplicationMapper {

    public LoanApplicationResponse toResponse(LoanApplication app) {
        return new LoanApplicationResponse(
                app.getId(),
                app.getApplicationNumber(),
                app.getStatus(),
                app.getCustomer().getId(),
                app.getCustomer().getUser().getFullName(),
                app.getProduct().getId(),
                app.getProduct().getName(),
                app.getLoanAmount(),
                app.getTenureMonths(),
                app.getInterestRate(),
                app.getMonthlyEmi(),
                app.getPurpose(),
                app.getReviewComment(),
                app.getDecisionComment(),
                nameOf(app.getSubmittedBy()),
                nameOf(app.getReviewedBy()),
                nameOf(app.getApprovedBy()),
                app.getSubmittedAt(),
                app.getReviewedAt(),
                app.getDecidedAt(),
                app.getCreatedAt());
    }

    public ApplicationHistoryResponse toHistoryResponse(ApplicationHistory history) {
        return new ApplicationHistoryResponse(
                history.getId(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getAction(),
                history.getComment(),
                history.getPerformedBy().getFullName(),
                history.getPerformedAt());
    }

    public List<ApplicationHistoryResponse> toHistoryResponses(List<ApplicationHistory> history) {
        return history.stream().map(this::toHistoryResponse).toList();
    }

    private String nameOf(com.apsfc.loanmanagement.entity.User user) {
        return user != null ? user.getFullName() : null;
    }
}