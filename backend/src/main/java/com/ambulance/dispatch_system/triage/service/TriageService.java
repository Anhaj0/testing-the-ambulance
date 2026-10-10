package com.ambulance.dispatch_system.triage.service;

import com.ambulance.dispatch_system.common.entity.Patient;
import com.ambulance.dispatch_system.common.entity.TriageAssessment;
import com.ambulance.dispatch_system.common.entity.enums.TriageCategory;
import com.ambulance.dispatch_system.common.entity.enums.UrgencyLevel;
import com.ambulance.dispatch_system.common.repository.PatientRepository;
import com.ambulance.dispatch_system.common.repository.TriageAssessmentRepository;
import com.ambulance.dispatch_system.triage.exception.PatientNotFoundException;
import com.ambulance.dispatch_system.triage.exception.TriageAssessmentNotFoundException;
import com.ambulance.dispatch_system.triage.dto.TriageRequestDTO;
import com.ambulance.dispatch_system.triage.dto.TriageResponseDTO;
import com.ambulance.dispatch_system.triage.service.impl.algorithms.MTSDecisionTree;
import com.ambulance.dispatch_system.triage.service.impl.algorithms.WeightedScoringStrategy;
import com.ambulance.dispatch_system.triage.util.PriorityDispatchQueue;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TriageService {

    private final MTSDecisionTree mtsDecisionTree;
    private final WeightedScoringStrategy scoringStrategy;
    private final TriageAssessmentRepository assessmentRepository;
    private final PatientRepository patientRepository;
    private final PriorityDispatchQueue dispatchQueue;

    public TriageService(MTSDecisionTree mtsDecisionTree,
                         WeightedScoringStrategy scoringStrategy,
                         TriageAssessmentRepository assessmentRepository,
                         PatientRepository patientRepository) {
        this.mtsDecisionTree = mtsDecisionTree;
        this.scoringStrategy = scoringStrategy;
        this.assessmentRepository = assessmentRepository;
        this.patientRepository = patientRepository;
        this.dispatchQueue = new PriorityDispatchQueue();
    }

    /** Restores unresolved assessments into the in-memory queue at startup. */
    @PostConstruct
    public void initQueue() {
        dispatchQueue.clear();
        assessmentRepository.findActiveQueue().forEach(dispatchQueue::insert);
    }

    /**
     * Evaluates and persists a patient's triage assessment. The patient's shared
     * urgency level is updated in the same transaction so dispatch sees the result.
     */
    @Transactional
    public TriageResponseDTO evaluate(TriageRequestDTO request) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new PatientNotFoundException(request.getPatientId()));

        TriageCategory category = mtsDecisionTree.evaluate(request);
        double score = scoringStrategy.calculateScore(request);

        patient.setUrgencyLevel(toUrgencyLevel(category));
        patientRepository.save(patient);

        TriageAssessment savedAssessment = assessmentRepository.save(
                createAssessment(request, patient, category, score));

        int position = dispatchQueue.getRank(savedAssessment);
        afterCommit(() -> dispatchQueue.insert(savedAssessment));

        return toResponse(savedAssessment, position);
    }

    private TriageResponseDTO toResponse(TriageAssessment assessment, int position) {
        TriageResponseDTO response = new TriageResponseDTO(
                assessment.getId(),
                assessment.getAssignedCategory(),
                assessment.getTieBreakerScore(),
                position,
                assessment.getCreatedAt()
        );
        if (assessment.getPatient() != null) {
            response.setPatientId(assessment.getPatient().getId());
            response.setPatientName(assessment.getPatient().getName());
        }
        return response;
    }

    private TriageAssessment createAssessment(TriageRequestDTO request,
                                               Patient patient,
                                               TriageCategory category,
                                               double score) {
        TriageAssessment assessment = new TriageAssessment();
        assessment.setPatient(patient);
        assessment.setBreathing(request.getBreathing());
        assessment.setPulseRate(request.getPulseRate());
        assessment.setAvpu(request.getAvpu());
        assessment.setOxygenSaturation(request.getOxygenSaturation());
        assessment.setSystolicBP(request.getSystolicBP());
        assessment.setPainScore(request.getPainScore());
        assessment.setTemperature(request.getTemperature());
        assessment.setAge(request.getAge());
        assessment.setHazardPresent(request.getHazardPresent());
        assessment.setSymptoms(request.getSymptoms());
        assessment.setAssignedCategory(category);
        assessment.setTieBreakerScore(score);
        return assessment;
    }

    public List<TriageResponseDTO> getActiveQueue() {
        List<TriageAssessment> activeAssessments = dispatchQueue.getSnapshot();
        List<TriageResponseDTO> response = new ArrayList<>(activeAssessments.size());

        int position = 1;
        for (TriageAssessment assessment : activeAssessments) {
            response.add(toResponse(assessment, position++));
        }
        return response;
    }

    @Transactional
    public void markResolved(UUID id) {
        TriageAssessment assessment = assessmentRepository.findById(id)
                .orElseThrow(() -> new TriageAssessmentNotFoundException(id));
        assessment.setResolved(true);
        assessmentRepository.save(assessment);

        afterCommit(() -> dispatchQueue.remove(id));
    }

    private UrgencyLevel toUrgencyLevel(TriageCategory category) {
        return switch (category) {
            case RED -> UrgencyLevel.CRITICAL;
            case ORANGE -> UrgencyLevel.HIGH;
            case YELLOW -> UrgencyLevel.MEDIUM;
            case GREEN, BLUE -> UrgencyLevel.LOW;
        };
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        action.run();
                    }
                }
        );
    }
}
