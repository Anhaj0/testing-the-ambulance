package com.ambulance.dispatch_system.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base type for every domain exception in this application. Carries the HTTP status
 * {@link GlobalExceptionHandler} should respond with, plus a short machine-readable error code,
 * so a single handler can format any subclass consistently without switching on its concrete
 * type. Domain modules keep their own exceptions in a per-domain `exception` package (e.g.
 * `routing.exception`, `triage.exception`) and extend this class; only exceptions that are
 * genuinely shared across domains belong directly in this `common.exception` package.
 */
public abstract class BaseException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    protected BaseException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
