package com.apsfc.loanmanagement.exception;

import org.springframework.http.HttpStatus;

/**
 * Object-level authorization failure (e.g. a customer accessing another
 * customer's resource).
 */
public class ForbiddenOperationException extends ApiException {

    public ForbiddenOperationException(String message) {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }
}