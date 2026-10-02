package com.apsfc.loanmanagement.repository;

import com.apsfc.loanmanagement.entity.Loan;
import com.apsfc.loanmanagement.enums.LoanStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public final class LoanSpecifications {

    private LoanSpecifications() {
    }

    public static Specification<Loan> belongsToCustomer(Long customerId) {
        return (root, query, cb) -> cb.equal(root.get("customer").get("id"), customerId);
    }

    public static Specification<Loan> hasStatus(LoanStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Loan> hasProduct(Long productId) {
        return (root, query, cb) -> cb.equal(root.get("product").get("id"), productId);
    }

    public static Specification<Loan> disbursedBetween(LocalDateTime from, LocalDateTime to) {
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