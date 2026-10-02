package com.apsfc.loanmanagement.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

/**
 * Consistent error envelope returned by the GlobalExceptionHandler.
 */
public record ErrorResponse(
        @Schema(example = "false") boolean success,
        String message,
        String errorCode,
        Instant timestamp,
        String path,
        Map<String, String> fieldErrors
) {

    public static ErrorResponse of(String message, String errorCode, Instant timestamp,
                                   String path, Map<String, String> fieldErrors) {
        return new ErrorResponse(false, message, errorCode, timestamp, path, fieldErrors);
    }

    public static ErrorResponse of(String message, String errorCode, Instant timestamp, String path) {
        return new ErrorResponse(false, message, errorCode, timestamp, path, null);
    }
}