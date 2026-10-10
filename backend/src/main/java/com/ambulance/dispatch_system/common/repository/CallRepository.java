package com.ambulance.dispatch_system.common.repository;

import com.ambulance.dispatch_system.common.entity.Call;
import com.ambulance.dispatch_system.common.entity.enums.CallStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CallRepository extends JpaRepository<Call, Long> {

    /** Used by the Decision (Triage) module to pull the current queue of unresolved calls. */
    List<Call> findByStatus(CallStatus status);
}
