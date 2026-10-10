package com.ambulance.dispatch_system.optimization.exception;

import com.ambulance.dispatch_system.common.exception.BaseException;
import org.springframework.http.HttpStatus;

/** A Staff record referenced by ID doesn't exist. */
public class StaffNotFoundException extends BaseException {

    public StaffNotFoundException(Long id) {
        super("Staff " + id + " not found", "SCHEDULE_E_002", HttpStatus.NOT_FOUND);
    }
}
