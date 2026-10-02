package com.apsfc.loanmanagement.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base application exception carrying an HTTP status and a machine-readable
 * error code. All domain exceptions extend this class so the
 * GlobalExceptionHandler can render a consistent error response.
 */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public ApiException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public ApiException(HttpStatus status, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
    }
}