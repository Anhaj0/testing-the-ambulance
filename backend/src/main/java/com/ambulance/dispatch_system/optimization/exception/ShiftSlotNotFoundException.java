package com.ambulance.dispatch_system.optimization.exception;

import com.ambulance.dispatch_system.common.exception.BaseException;
import org.springframework.http.HttpStatus;

/** A ShiftSlot record referenced by ID doesn't exist. */
public class ShiftSlotNotFoundException extends BaseException {

    public ShiftSlotNotFoundException(Long id) {
        super("ShiftSlot " + id + " not found", "SCHEDULE_E_003", HttpStatus.NOT_FOUND);
    }
}
