package com.ambulance.dispatch_system.resource_allocation.service;

import com.ambulance.dispatch_system.common.entity.Ambulance;
import com.ambulance.dispatch_system.common.entity.Call;
import com.ambulance.dispatch_system.common.entity.enums.AmbulanceStatus;
import com.ambulance.dispatch_system.common.entity.enums.CallStatus;
import com.ambulance.dispatch_system.common.repository.AmbulanceRepository;
import com.ambulance.dispatch_system.common.repository.CallRepository;
import com.ambulance.dispatch_system.resource_allocation.dto.AmbulanceDto;
import com.ambulance.dispatch_system.resource_allocation.dto.CallDto;
import com.ambulance.dispatch_system.resource_allocation.dto.CandidateDto;
import com.ambulance.dispatch_system.resource_allocation.dto.DispatchResultDto;
import com.ambulance.dispatch_system.resource_allocation.exception.CallNotFoundException;
import com.ambulance.dispatch_system.resource_allocation.optimization.GreedyScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class DispatchService {

    private final CallRepository callRepository;
    private final AmbulanceRepository ambulanceRepository;
    private final GreedyScheduler greedyScheduler;

    public DispatchService(CallRepository callRepository,
            AmbulanceRepository ambulanceRepository,
            GreedyScheduler greedyScheduler) {
        this.callRepository = callRepository;
        this.ambulanceRepository = ambulanceRepository;
        this.greedyScheduler = greedyScheduler;
    }

    @Transactional
    public DispatchResultDto handleEmergencyDispatch(Long callId) {
        Call call = callRepository.findById(callId)
                .orElseThrow(() -> new CallNotFoundException(callId));

        Optional<Ambulance> bestAmbulanceOpt = greedyScheduler.findBestAmbulance(
                call.getLocationNode(),
                call.getRequiredEquipment());

        if (bestAmbulanceOpt.isEmpty()) {
            return new DispatchResultDto(false, callId, null, "No suitable ambulance available at this time.");
        }

        Ambulance bestAmbulance = bestAmbulanceOpt.get();

        call.setAssignedAmbulance(bestAmbulance);
        call.setStatus(CallStatus.DISPATCHED);

        bestAmbulance.setStatus(AmbulanceStatus.DISPATCHED);

        ambulanceRepository.save(bestAmbulance);
        callRepository.save(call);

        return new DispatchResultDto(true, callId, bestAmbulance.getVehicleNumber(),
                "Ambulance " + bestAmbulance.getVehicleNumber() + " dispatched successfully.");
    }

    /**
     * Ranks every eligible ambulance for a call exactly as {@link #handleEmergencyDispatch}
     * would, but without assigning anything - a read-only preview of the greedy scheduler's
     * decision for a dispatch-board UI.
     */
    public List<CandidateDto> getCandidates(Long callId) {
        Call call = callRepository.findById(callId)
                .orElseThrow(() -> new CallNotFoundException(callId));

        return greedyScheduler.rankCandidates(call.getLocationNode(), call.getRequiredEquipment()).stream()
                .map(CandidateDto::fromScored)
                .toList();
    }

    public List<CallDto> getPendingCalls() {
        return callRepository.findByStatus(CallStatus.RECEIVED).stream().map(CallDto::fromEntity).toList();
    }

    public List<AmbulanceDto> getAllAmbulances() {
        return ambulanceRepository.findAll().stream().map(AmbulanceDto::fromEntity).toList();
    }
}
