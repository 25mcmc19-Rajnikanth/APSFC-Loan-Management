package com.apsfc.loanmanagement.repository;

import com.apsfc.loanmanagement.dto.response.CollectionSummaryRow;
import com.apsfc.loanmanagement.entity.Repayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface RepaymentRepository extends JpaRepository<Repayment, Long>, JpaSpecificationExecutor<Repayment> {

    Page<Repayment> findByLoanIdOrderByPaymentDateDesc(Long loanId, Pageable pageable);

    long countByLoanId(Long loanId);

    @Query("select coalesce(sum(r.amount), 0) from Repayment r")
    BigDecimal sumAll();

    @Query("select coalesce(sum(r.amount), 0) from Repayment r where r.paymentDate between :from and :to")
    BigDecimal sumBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select new com.apsfc.loanmanagement.dto.response.CollectionSummaryRow(
                r.paymentMode, count(r), coalesce(sum(r.amount), 0))
            from Repayment r where r.paymentDate between :from and :to
            group by r.paymentMode
            order by coalesce(sum(r.amount), 0) desc
            """)
    List<CollectionSummaryRow> collectionSummary(@Param("from") LocalDate from, @Param("to") LocalDate to);
}