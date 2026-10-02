package com.apsfc.loanmanagement.exception;

import org.springframework.http.HttpStatus;

public class InvalidRepaymentException extends ApiException {

    public InvalidRepaymentException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_REPAYMENT", message);
    }
}