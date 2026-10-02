package com.apsfc.loanmanagement.repository;

import com.apsfc.loanmanagement.dto.response.CustomerLoanAggregate;
import com.apsfc.loanmanagement.dto.response.ProductPortfolioRow;
import com.apsfc.loanmanagement.entity.Loan;
import com.apsfc.loanmanagement.enums.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, Long>, JpaSpecificationExecutor<Loan> {

    Optional<Loan> findByApplicationId(Long applicationId);

    List<Loan> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    long countByStatus(LoanStatus status);

    @Query("select coalesce(sum(l.outstandingBalance), 0) from Loan l where l.status = :status")
    BigDecimal sumOutstandingByStatus(@Param("status") LoanStatus status);

    @Query("select coalesce(sum(l.principalAmount), 0) from Loan l")
    BigDecimal sumPrincipal();

    @Query("select coalesce(sum(l.principalAmount), 0) from Loan l where l.disbursementDate between :from and :to")
    BigDecimal sumPrincipalBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select new com.apsfc.loanmanagement.dto.response.ProductPortfolioRow(
                l.product.id, l.product.name, count(l),
                coalesce(sum(l.principalAmount), 0), coalesce(sum(l.outstandingBalance), 0))
            from Loan l where l.status = :status
            group by l.product.id, l.product.name
            order by coalesce(sum(l.outstandingBalance), 0) desc
            """)
    List<ProductPortfolioRow> portfolioByStatus(@Param("status") LoanStatus status);

    @Query("""
            select new com.apsfc.loanmanagement.dto.response.CustomerLoanAggregate(
                l.customer.id, count(l), coalesce(sum(l.outstandingBalance), 0))
            from Loan l where l.status = :status
            group by l.customer.id
            """)
    List<CustomerLoanAggregate> loanAggregatesByStatus(@Param("status") LoanStatus status);
}