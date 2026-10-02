package com.apsfc.loanmanagement.repository;

import com.apsfc.loanmanagement.entity.Employee;
import com.apsfc.loanmanagement.enums.Role;
import com.apsfc.loanmanagement.enums.UserStatus;
import org.springframework.data.jpa.domain.Specification;

public final class EmployeeSpecifications {

    private EmployeeSpecifications() {
    }

    public static Specification<Employee> hasRole(Role role) {
        return (root, query, cb) -> cb.equal(root.get("user").get("role"), role);
    }

    public static Specification<Employee> hasStatus(UserStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Employee> search(String term) {
        if (term == null || term.isBlank()) {
            return Specification.where(null);
        }
        String like = "%" + term.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("user").get("fullName")), like),
                cb.like(cb.lower(root.get("user").get("username")), like),
                cb.like(cb.lower(root.get("employeeCode")), like),
                cb.like(root.get("user").get("phone"), like));
    }
}