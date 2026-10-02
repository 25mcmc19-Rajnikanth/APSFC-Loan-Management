package com.apsfc.loanmanagement.repository;

import com.apsfc.loanmanagement.entity.LoanProduct;
import com.apsfc.loanmanagement.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoanProductRepository extends JpaRepository<LoanProduct, Long> {

    Optional<LoanProduct> findByCode(String code);

    boolean existsByCode(String code);

    Page<LoanProduct> findByStatus(ProductStatus status, Pageable pageable);
}