package com.ambulance.dispatch_system.common.exception;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * The JSON body returned for any exception GlobalExceptionHandler handles. {@code details} is
 * null for a plain error (e.g. a domain {@link BaseException}) and populated with per-field
 * messages for request validation failures, so every error response shares one shape instead of
 * validation failures returning a differently-structured body.
 */
public record ErrorResponse(
        String errorCode,
        String message,
        int status,
        LocalDateTime timestamp,
        Map<String, String> details
) {

    public static ErrorResponse of(BaseException ex) {
        return new ErrorResponse(ex.getErrorCode(), ex.getMessage(), ex.getHttpStatus().value(), LocalDateTime.now(), null);
    }

    public static ErrorResponse ofValidation(Map<String, String> fieldErrors) {
        return new ErrorResponse("VALIDATION_ERROR", "Validation failed", 400, LocalDateTime.now(), fieldErrors);
    }
}
