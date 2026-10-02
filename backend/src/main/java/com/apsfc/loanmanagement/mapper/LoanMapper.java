package com.apsfc.loanmanagement.mapper;

import com.apsfc.loanmanagement.dto.response.EmiScheduleResponse;
import com.apsfc.loanmanagement.dto.response.LoanResponse;
import com.apsfc.loanmanagement.entity.EmiSchedule;
import com.apsfc.loanmanagement.entity.Loan;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LoanMapper {

    public LoanResponse toResponse(Loan loan) {
        return new LoanResponse(
                loan.getId(),
                loan.getLoanNumber(),
                loan.getApplication().getId(),
                loan.getApplication().getApplicationNumber(),
                loan.getCustomer().getId(),
                loan.getCustomer().getUser().getFullName(),
                loan.getProduct() != null ? loan.getProduct().getId() : null,
                loan.getProduct() != null ? loan.getProduct().getName() : null,
                loan.getPrincipalAmount(),
                loan.getInterestRate(),
                loan.getTenureMonths(),
                loan.getMonthlyEmi(),
                loan.getTotalInterest(),
                loan.getTotalAmount(),
                loan.getDisbursedAmount(),
                loan.getDisbursementDate(),
                loan.getAmountPaid(),
                loan.getOutstandingBalance(),
                loan.getStatus(),
                loan.getDisbursedBy() != null ? loan.getDisbursedBy().getFullName() : null,
                loan.getClosedAt(),
                loan.getCreatedAt());
    }

    public EmiScheduleResponse toEmiResponse(EmiSchedule emi) {
        return new EmiScheduleResponse(
                emi.getId(),
                emi.getEmiNumber(),
                emi.getDueDate(),
                emi.getPrincipalComponent(),
                emi.getInterestComponent(),
                emi.getTotalAmount(),
                emi.getCumulativePaid(),
                emi.getPaymentStatus(),
                emi.getPaidDate());
    }

    public List<EmiScheduleResponse> toEmiResponses(List<EmiSchedule> schedule) {
        return schedule.stream().map(this::toEmiResponse).toList();
    }
}