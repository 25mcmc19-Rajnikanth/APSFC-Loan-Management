package com.apsfc.loanmanagement.exception;

import org.springframework.http.HttpStatus;

/**
 * Bad credentials or unknown login identifier. Careful not to disclose which
 * one failed.
 */
public class AuthenticationFailedException extends ApiException {

    public AuthenticationFailedException() {
        super(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid username/email or password");
    }
}