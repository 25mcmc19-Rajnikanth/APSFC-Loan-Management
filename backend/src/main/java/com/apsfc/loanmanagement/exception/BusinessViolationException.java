package com.apsfc.loanmanagement.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a business rule is violated (amount/tenure out of product range,
 * duplicate application, repayment on a closed loan, etc.).
 */
public class BusinessViolationException extends ApiException {

    public BusinessViolationException(String message) {
        super(HttpStatus.BAD_REQUEST, "BUSINESS_VIOLATION", message);
    }
}