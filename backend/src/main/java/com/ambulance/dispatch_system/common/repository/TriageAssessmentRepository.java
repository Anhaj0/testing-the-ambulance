package com.ambulance.dispatch_system.common.repository;

import com.ambulance.dispatch_system.common.entity.TriageAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TriageAssessmentRepository extends JpaRepository<TriageAssessment, UUID> {

    @Query("SELECT t FROM TriageAssessment t JOIN FETCH t.patient WHERE t.resolved = false " +
           "ORDER BY t.severityRank DESC, t.tieBreakerScore DESC, t.createdAt ASC")
    List<TriageAssessment> findActiveQueue();
}
