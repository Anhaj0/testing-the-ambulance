package com.ambulance.dispatch_system.resource_allocation.exception;

import com.ambulance.dispatch_system.common.exception.BaseException;
import org.springframework.http.HttpStatus;

/** The emergency Call referenced by a dispatch request doesn't exist. */
public class CallNotFoundException extends BaseException {

    public CallNotFoundException(Long callId) {
        super("Call not found with ID: " + callId, "DISPATCH_E_001", HttpStatus.NOT_FOUND);
    }
}
