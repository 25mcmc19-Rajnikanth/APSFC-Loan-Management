package com.apsfc.loanmanagement.repository;

import com.apsfc.loanmanagement.dto.response.EmiDueResponse;
import com.apsfc.loanmanagement.entity.EmiSchedule;
import com.apsfc.loanmanagement.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EmiScheduleRepository extends JpaRepository<EmiSchedule, Long> {

    List<EmiSchedule> findByLoanIdOrderByEmiNumberAsc(Long loanId);

    Optional<EmiSchedule> findByLoanIdAndEmiNumber(Long loanId, int emiNumber);

    List<EmiSchedule> findByLoanIdAndPaymentStatusOrderByEmiNumberAsc(Long loanId, PaymentStatus paymentStatus);

    EmiSchedule findFirstByLoanIdAndPaymentStatusOrderByEmiNumberAsc(Long loanId, PaymentStatus paymentStatus);

    List<EmiSchedule> findByLoanIdAndPaymentStatusInOrderByEmiNumberAsc(
            Long loanId, Collection<PaymentStatus> statuses);

    @Query("""
            select new com.apsfc.loanmanagement.dto.response.EmiDueResponse(
                l.id, l.loanNumber, c.id, c.user.fullName, e.id, e.emiNumber, e.dueDate,
                e.totalAmount, e.cumulativePaid, e.totalAmount - e.cumulativePaid, e.paymentStatus)
            from EmiSchedule e
            join e.loan l
            join e.loan.customer c
            where e.dueDate between :from and :to and e.paymentStatus in :statuses
            order by e.dueDate asc, e.emiNumber asc
            """)
    List<EmiDueResponse> findDueBetween(
            @Param("from") LocalDate from, @Param("to") LocalDate to,
            @Param("statuses") Collection<PaymentStatus> statuses);

    @Query("""
            select new com.apsfc.loanmanagement.dto.response.EmiDueResponse(
                l.id, l.loanNumber, c.id, c.user.fullName, e.id, e.emiNumber, e.dueDate,
                e.totalAmount, e.cumulativePaid, e.totalAmount - e.cumulativePaid, e.paymentStatus)
            from EmiSchedule e
            join e.loan l
            join e.loan.customer c
            where e.dueDate < :before and e.paymentStatus in :statuses
            order by e.dueDate asc, e.emiNumber asc
            """)
    List<EmiDueResponse> findDueBefore(
            @Param("before") LocalDate before,
            @Param("statuses") Collection<PaymentStatus> statuses);
}