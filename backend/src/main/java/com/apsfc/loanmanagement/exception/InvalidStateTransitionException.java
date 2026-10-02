package com.apsfc.loanmanagement.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a state transition is attempted that is not allowed by the
 * loan-application or loan state machine.
 */
public class InvalidStateTransitionException extends ApiException {

    public InvalidStateTransitionException(String message) {
        super(HttpStatus.CONFLICT, "INVALID_STATE_TRANSITION", message);
    }
}