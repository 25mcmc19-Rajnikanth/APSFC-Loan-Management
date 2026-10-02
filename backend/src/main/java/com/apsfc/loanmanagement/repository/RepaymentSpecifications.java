package com.apsfc.loanmanagement.repository;

import com.apsfc.loanmanagement.entity.Repayment;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public final class RepaymentSpecifications {

    public static Specification<Repayment> onLoan(Long loanId) {
        return (root, query, cb) -> cb.equal(root.get("loan").get("id"), loanId);
    }

    public static Specification<Repayment> ofCustomer(Long customerId) {
        return (root, query, cb) -> cb.equal(root.get("loan").get("customer").get("id"), customerId);
    }

    public static Specification<Repayment> paidBetween(LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            if (from != null && to != null) {
                return cb.between(root.get("createdAt"), from, to);
            }
            if (from != null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), from);
            }
            if (to != null) {
                return cb.lessThanOrEqualTo(root.get("createdAt"), to);
            }
            return cb.conjunction();
        };
    }
}