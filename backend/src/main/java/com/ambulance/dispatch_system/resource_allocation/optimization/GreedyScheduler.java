package com.ambulance.dispatch_system.resource_allocation.optimization;

import com.ambulance.dispatch_system.common.entity.Ambulance;
import com.ambulance.dispatch_system.common.entity.RoadNode;
import com.ambulance.dispatch_system.common.entity.enums.AmbulanceStatus;
import com.ambulance.dispatch_system.common.entity.enums.MedicalEquipment;
import com.ambulance.dispatch_system.common.repository.AmbulanceRepository;
import com.ambulance.dispatch_system.common.repository.RoadNodeRepository;
import com.ambulance.dispatch_system.routing.service.RoutingSnapshot;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class GreedyScheduler {

    private final AmbulanceRepository ambulanceRepository;
    private final FitnessEvaluator fitnessEvaluator;

    public GreedyScheduler(AmbulanceRepository ambulanceRepository, FitnessEvaluator fitnessEvaluator) {
        this.ambulanceRepository = ambulanceRepository;
        this.fitnessEvaluator = fitnessEvaluator;
    }

    public Optional<Ambulance> findBestAmbulance(String patientNode, Set<MedicalEquipment> requiredEquipment) {
        return rankCandidates(patientNode, requiredEquipment).stream()
                .findFirst()
                .map(ScoredAmbulance::ambulance);
    }

    /**
     * Scores every equipment-eligible, reachable AVAILABLE ambulance against a call and returns
     * them best-first. Used both to pick the winner (see {@link #findBestAmbulance}) and to let
     * callers preview the ranking - e.g. a dispatch-board UI - without committing to it, since
     * scoring here never touches the database beyond the initial read.
     */
    public List<ScoredAmbulance> rankCandidates(String patientNode, Set<MedicalEquipment> requiredEquipment) {
        List<Ambulance> availableAmbulances = ambulanceRepository.findByStatus(AmbulanceStatus.AVAILABLE);

        // Load the road network once for this whole dispatch decision. Scoring each candidate
        // through FitnessEvaluator's routeService-backed overload would otherwise re-fetch the
        // full edge list from the database on every single ambulance instead of once overall.
        RoutingSnapshot routingSnapshot = fitnessEvaluator.newRoutingSnapshot();

        // Score each candidate exactly once, then sort - re-running the shortest-path search on
        // both sides of every Comparator.comparingDouble comparison would cost a fleet of n
        // ambulances roughly 2n graph traversals instead of n.
        return availableAmbulances.stream()
                .filter(amb -> amb.getEquipment().containsAll(requiredEquipment))
                .map(amb -> new ScoredAmbulance(amb, fitnessEvaluator.evaluate(
                        amb, patientNode, requiredEquipment, routingSnapshot)))
                .filter(scored -> scored.score() < FitnessEvaluator.UNREACHABLE)
                .sorted(Comparator.comparingDouble(ScoredAmbulance::score))
                .toList();
    }

    /** An ambulance paired with its already-computed fitness breakdown. */
    public record ScoredAmbulance(Ambulance ambulance, FitnessEvaluator.FitnessBreakdown fitness) {
        public double score() {
            return fitness.score();
        }
    }
}