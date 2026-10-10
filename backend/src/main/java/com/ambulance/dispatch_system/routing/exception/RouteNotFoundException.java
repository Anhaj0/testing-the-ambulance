package com.ambulance.dispatch_system.routing.exception;

import com.ambulance.dispatch_system.common.exception.BaseException;
import org.springframework.http.HttpStatus;

/** Both locations exist, but no unblocked path connects them. */
public class RouteNotFoundException extends BaseException {

    public RouteNotFoundException(String message) {
        super(message, "ROUTE_E_002", HttpStatus.NOT_FOUND);
    }
}
