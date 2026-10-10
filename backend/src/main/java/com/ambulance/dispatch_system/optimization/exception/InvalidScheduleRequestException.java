package com.ambulance.dispatch_system.optimization.exception;

import com.ambulance.dispatch_system.common.exception.BaseException;
import org.springframework.http.HttpStatus;

/** A /run, /compare, or roster-build request that can't be fulfilled as specified. */
public class InvalidScheduleRequestException extends BaseException {

    public InvalidScheduleRequestException(String message) {
        super(message, "SCHEDULE_E_001", HttpStatus.BAD_REQUEST);
    }
}
