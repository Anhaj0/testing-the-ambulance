package com.ambulance.dispatch_system.routing.exception;

import com.ambulance.dispatch_system.common.exception.BaseException;
import org.springframework.http.HttpStatus;

public class DrivingRouteUnavailableException extends BaseException {
    public DrivingRouteUnavailableException(String message) {
        super(message, "ROUTE_E_003", HttpStatus.SERVICE_UNAVAILABLE);
    }
}
