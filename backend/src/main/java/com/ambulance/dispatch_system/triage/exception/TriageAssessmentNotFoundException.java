package com.ambulance.dispatch_system.triage.exception;

import com.ambulance.dispatch_system.common.exception.BaseException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

/** The triage assessment referenced (e.g. when resolving it) doesn't exist. */
public class TriageAssessmentNotFoundException extends BaseException {

    public TriageAssessmentNotFoundException(UUID id) {
        super("Triage assessment not found with ID: " + id, "TRIAGE_E_002", HttpStatus.NOT_FOUND);
    }
}
