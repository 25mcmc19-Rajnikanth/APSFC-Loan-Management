package com.apsfc.loanmanagement.repository;

import com.apsfc.loanmanagement.entity.ApplicationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationHistoryRepository extends JpaRepository<ApplicationHistory, Long> {

    List<ApplicationHistory> findByApplicationIdOrderByPerformedAtAsc(Long applicationId);
}