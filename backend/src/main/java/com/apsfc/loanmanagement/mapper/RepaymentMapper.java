package com.apsfc.loanmanagement.mapper;

import com.apsfc.loanmanagement.dto.response.RepaymentResponse;
import com.apsfc.loanmanagement.entity.Repayment;
import org.springframework.stereotype.Component;

@Component
public class RepaymentMapper {

    public RepaymentResponse toResponse(Repayment repayment) {
        return new RepaymentResponse(
                repayment.getId(),
                repayment.getLoan().getId(),
                repayment.getLoan().getLoanNumber(),
                repayment.getEmiAllocation() != null ? repayment.getEmiAllocation().getId() : null,
                repayment.getEmiAllocation() != null ? repayment.getEmiAllocation().getEmiNumber() : null,
                repayment.getAmount(),
                repayment.getPaymentMode(),
                repayment.getTransactionRef(),
                repayment.getPaymentDate(),
                repayment.getRemarks(),
                repayment.getCreatedBy() != null ? repayment.getCreatedBy().getFullName() : null,
                repayment.getCreatedAt());
    }
}