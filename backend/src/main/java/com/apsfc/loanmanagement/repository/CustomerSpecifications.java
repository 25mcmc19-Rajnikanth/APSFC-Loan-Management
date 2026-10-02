package com.apsfc.loanmanagement.repository;

import com.apsfc.loanmanagement.entity.Customer;
import com.apsfc.loanmanagement.enums.UserStatus;
import org.springframework.data.jpa.domain.Specification;

public final class CustomerSpecifications {

    private CustomerSpecifications() {
    }

    public static Specification<Customer> hasStatus(UserStatus status) {
        return (root, query, cb) -> cb.equal(root.get("user").get("status"), status);
    }

    public static Specification<Customer> kycStatus(Boolean verified) {
        return (root, query, cb) -> cb.equal(root.get("kycVerified"), verified);
    }

    public static Specification<Customer> search(String term) {
        if (term == null || term.isBlank()) {
            return Specification.where(null);
        }
        String like = "%" + term.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("user").get("fullName")), like),
                cb.like(cb.lower(root.get("user").get("username")), like),
                cb.like(root.get("user").get("phone"), like),
                cb.like(cb.lower(root.get("panNumber")), like),
                cb.like(root.get("aadhaarNumber"), like),
                cb.like(cb.lower(root.get("city")), like));
    }
}