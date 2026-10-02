package com.apsfc.loanmanagement.repository;

import com.apsfc.loanmanagement.entity.LoanApplication;
import com.apsfc.loanmanagement.enums.ApplicationStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

/**
 * Reusable query specifications for filtering loan applications. Used only
 * where the filtering need is genuine (staff list, customer list) instead of
 * loading every row and filtering in memory.
 */
public final class LoanApplicationSpecifications {

    private LoanApplicationSpecifications() {
    }

    public static Specification<LoanApplication> hasStatus(ApplicationStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<LoanApplication> hasProduct(Long productId) {
        return (root, query, cb) -> cb.equal(root.get("product").get("id"), productId);
    }

    public static Specification<LoanApplication> belongsToCustomer(Long customerId) {
        return (root, query, cb) -> cb.equal(root.get("customer").get("id"), customerId);
    }

    public static Specification<LoanApplication> createdBetween(LocalDateTime from, LocalDateTime to) {
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