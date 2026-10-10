package com.ambulance.dispatch_system.routing.exception;

import com.ambulance.dispatch_system.common.exception.BaseException;
import org.springframework.http.HttpStatus;

/** A start/destination location referenced by ID or name doesn't exist in the road network. */
public class LocationNotFoundException extends BaseException {

    public LocationNotFoundException(String message) {
        super(message, "ROUTE_E_001", HttpStatus.BAD_REQUEST);
    }
}
