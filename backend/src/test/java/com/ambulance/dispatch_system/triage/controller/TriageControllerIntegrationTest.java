package com.ambulance.dispatch_system.triage.controller;

import com.ambulance.dispatch_system.common.entity.Patient;
import com.ambulance.dispatch_system.common.entity.TriageAssessment;
import com.ambulance.dispatch_system.common.entity.enums.ConsciousnessLevel;
import com.ambulance.dispatch_system.common.entity.enums.TriageCategory;
import com.ambulance.dispatch_system.common.entity.enums.UrgencyLevel;
import com.ambulance.dispatch_system.common.repository.PatientRepository;
import com.ambulance.dispatch_system.common.repository.TriageAssessmentRepository;
import com.ambulance.dispatch_system.triage.dto.TriageRequestDTO;
import com.ambulance.dispatch_system.triage.service.TriageService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TriageControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private TriageAssessmentRepository assessmentRepository;
    
    @Autowired
    private TriageService triageService;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;


    @BeforeEach
    void setUp() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        assessmentRepository.deleteAll();
        patientRepository.deleteAll();
        // Clear the in-memory queue to isolate tests
        triageService.initQueue();
    }

    @Autowired
    private tools.jackson.databind.ObjectMapper objectMapper;

    @Test
    void testEvaluateReturnsCorrectQueuePosition() throws Exception {
        // First patient (RED)
        TriageRequestDTO req1 = new TriageRequestDTO();
        Patient patient1 = createPatient("Patient One", 30);
        req1.setPatientId(patient1.getId());
        req1.setBreathing(false);
        req1.setPulseRate(0);
        req1.setAvpu(ConsciousnessLevel.UNRESPONSIVE);
        req1.setOxygenSaturation(95);
        req1.setSystolicBP(120);
        req1.setPainScore(0);
        req1.setTemperature(37.0);
        req1.setAge(30);
        req1.setHazardPresent(false);

        mockMvc.perform(post("/api/v1/triage/assessments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.patientId").value(patient1.getId().intValue()))
                .andExpect(jsonPath("$.patientName").value("Patient One"))
                .andExpect(jsonPath("$.category").value(TriageCategory.RED.name()))
                .andExpect(jsonPath("$.queuePosition").value(1));

        // Second patient (BLUE)
        TriageRequestDTO req2 = new TriageRequestDTO();
        Patient patient2 = createPatient("Patient Two", 25);
        req2.setPatientId(patient2.getId());
        req2.setBreathing(true);
        req2.setPulseRate(80);
        req2.setAvpu(ConsciousnessLevel.ALERT);
        req2.setOxygenSaturation(98);
        req2.setSystolicBP(120);
        req2.setPainScore(2);
        req2.setTemperature(37.0);
        req2.setAge(25);
        req2.setHazardPresent(false);

        mockMvc.perform(post("/api/v1/triage/assessments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value(TriageCategory.GREEN.name()))
                .andExpect(jsonPath("$.queuePosition").value(2)); // behind RED

        // Third patient (ORANGE)
        TriageRequestDTO req3 = new TriageRequestDTO();
        Patient patient3 = createPatient("Patient Three", 40);
        req3.setPatientId(patient3.getId());
        req3.setBreathing(true);
        req3.setPulseRate(130);
        req3.setAvpu(ConsciousnessLevel.VOICE);
        req3.setOxygenSaturation(95);
        req3.setSystolicBP(120);
        req3.setPainScore(8);
        req3.setTemperature(37.0);
        req3.setAge(40);
        req3.setHazardPresent(false);

        // Should rank 2nd (behind RED, ahead of BLUE)
        mockMvc.perform(post("/api/v1/triage/assessments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value(TriageCategory.ORANGE.name()))
                .andExpect(jsonPath("$.queuePosition").value(2));
                
        // Test /queue endpoint correctly returns heap snapshot
        mockMvc.perform(get("/api/v1/triage/assessments/queue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id").isNotEmpty())
                .andExpect(jsonPath("$[0].patientId").value(patient1.getId().intValue()))
                .andExpect(jsonPath("$[1].patientId").value(patient3.getId().intValue()))
                .andExpect(jsonPath("$[0].category").value("RED"))
                .andExpect(jsonPath("$[1].category").value("ORANGE"))
                .andExpect(jsonPath("$[2].category").value("GREEN"));

        TriageAssessment patientAssessment = assessmentRepository.findAll().stream()
                .filter(assessment -> assessment.getPatient().getId().equals(patient1.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals(patient1.getId(), patientAssessment.getPatient().getId());
        assertEquals(UrgencyLevel.CRITICAL,
                patientRepository.findById(patient1.getId()).orElseThrow().getUrgencyLevel());
    }
    
    @Test
    void testInvalidPayloadReturns400() throws Exception {
        String invalidReq = "{\"breathing\":true,\"painScore\":15}"; // Out of range

        mockMvc.perform(post("/api/v1/triage/assessments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidReq))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testMarkResolvedRemovesFromQueue() throws Exception {
        Patient patient = createPatient("Patient", 30);
        String req1 = "{\"patientId\":" + patient.getId()
                + ",\"breathing\":false,\"pulseRate\":0,\"avpu\":\"UNRESPONSIVE\""
                + ",\"oxygenSaturation\":95,\"systolicBP\":120,\"painScore\":0"
                + ",\"temperature\":37.0,\"age\":30,\"hazardPresent\":false}";

        mockMvc.perform(post("/api/v1/triage/assessments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req1))
                .andExpect(status().isCreated());
                
        // Fetch ID from repo
        TriageAssessment assessment = assessmentRepository.findAll().get(0);
        UUID id = assessment.getId();
        
        // Assert in queue
        mockMvc.perform(get("/api/v1/triage/assessments/queue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
                
        // Resolve
        mockMvc.perform(put("/api/v1/triage/assessments/" + id + "/resolve"))
                .andExpect(status().isNoContent());
                
        // Assert resolved in DB
        TriageAssessment resolvedAssessment = assessmentRepository.findById(id).orElseThrow();
        assertTrue(resolvedAssessment.getResolved());
        
        // Assert not in queue
        mockMvc.perform(get("/api/v1/triage/assessments/queue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testQueueChangesOnlyAfterSuccessfulCommit() {
        Patient rolledBackPatient = createPatient("Rollback Patient", 30);
        TriageRequestDTO rolledBackRequest = createRedRequest(rolledBackPatient.getId());
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        transaction.executeWithoutResult(status -> {
            triageService.evaluate(rolledBackRequest);
            status.setRollbackOnly();
        });

        assertTrue(assessmentRepository.findAll().isEmpty());
        assertTrue(triageService.getActiveQueue().isEmpty());
        assertNull(patientRepository.findById(rolledBackPatient.getId())
                .orElseThrow().getUrgencyLevel());

        Patient committedPatient = createPatient("Committed Patient", 30);
        triageService.evaluate(createRedRequest(committedPatient.getId()));
        TriageAssessment committedAssessment = assessmentRepository.findAll().get(0);

        transaction.executeWithoutResult(status -> {
            triageService.markResolved(committedAssessment.getId());
            status.setRollbackOnly();
        });

        assertFalse(assessmentRepository.findById(committedAssessment.getId())
                .orElseThrow().getResolved());
        assertEquals(1, triageService.getActiveQueue().size());
    }

    @Test
    void testHeapRehydrationOnStartup() throws Exception {
        // Bypass heap and insert directly into DB to simulate existing unresolved assessments
        TriageAssessment a1 = createPersistableAssessment(TriageCategory.RED, 15.0);
        assessmentRepository.saveAndFlush(a1);
        
        TriageAssessment a2 = createPersistableAssessment(TriageCategory.GREEN, 5.0);
        assessmentRepository.saveAndFlush(a2);
        
        // Call the @PostConstruct method manually to simulate app startup
        triageService.initQueue();
        
        // Assert the heap is correctly ordered
        mockMvc.perform(get("/api/v1/triage/assessments/queue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].patientId").value(a1.getPatient().getId().intValue()))
                .andExpect(jsonPath("$[0].patientName").value("Persisted Patient"))
                .andExpect(jsonPath("$[0].category").value("RED"))
                .andExpect(jsonPath("$[1].category").value("GREEN"));
    }

    private TriageAssessment createPersistableAssessment(TriageCategory category, double score) {
        TriageAssessment assessment = new TriageAssessment();
        assessment.setPatient(createPatient("Persisted Patient", 30));
        assessment.setBreathing(true);
        assessment.setPulseRate(80);
        assessment.setAvpu(ConsciousnessLevel.ALERT);
        assessment.setOxygenSaturation(98);
        assessment.setSystolicBP(120);
        assessment.setPainScore(2);
        assessment.setTemperature(37.0);
        assessment.setAge(30);
        assessment.setHazardPresent(false);
        assessment.setAssignedCategory(category);
        assessment.setTieBreakerScore(score);
        return assessment;
    }

    private Patient createPatient(String name, int age) {
        Patient patient = new Patient();
        patient.setName(name);
        patient.setAge(age);
        patient.setCondition("Awaiting triage");
        return patientRepository.saveAndFlush(patient);
    }

    private TriageRequestDTO createRedRequest(Long patientId) {
        TriageRequestDTO request = new TriageRequestDTO();
        request.setPatientId(patientId);
        request.setBreathing(false);
        request.setPulseRate(0);
        request.setAvpu(ConsciousnessLevel.UNRESPONSIVE);
        request.setOxygenSaturation(95);
        request.setSystolicBP(120);
        request.setPainScore(0);
        request.setTemperature(37.0);
        request.setAge(30);
        request.setHazardPresent(false);
        return request;
    }
}
