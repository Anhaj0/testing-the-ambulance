package com.ambulance.dispatch_system.triage.exception;

import com.ambulance.dispatch_system.common.exception.BaseException;
import org.springframework.http.HttpStatus;

/** The patient referenced by a triage request doesn't exist. */
public class PatientNotFoundException extends BaseException {

    public PatientNotFoundException(Long patientId) {
        super("Patient not found with ID: " + patientId, "TRIAGE_E_001", HttpStatus.NOT_FOUND);
    }
}
