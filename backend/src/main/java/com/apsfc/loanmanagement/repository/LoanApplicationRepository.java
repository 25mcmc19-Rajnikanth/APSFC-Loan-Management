package com.apsfc.loanmanagement.repository;

import com.apsfc.loanmanagement.dto.response.StatusCount;
import com.apsfc.loanmanagement.entity.LoanApplication;
import com.apsfc.loanmanagement.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface LoanApplicationRepository
        extends JpaRepository<LoanApplication, Long>, JpaSpecificationExecutor<LoanApplication> {

    List<LoanApplication> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    boolean existsByCustomerIdAndStatusIn(Long customerId, Collection<ApplicationStatus> statuses);

    long countByStatus(ApplicationStatus status);

    @Query("""
            select new com.apsfc.loanmanagement.dto.response.StatusCount(a.status, count(a))
            from LoanApplication a group by a.status
            """)
    List<StatusCount> countByStatusGrouped();
}