package com.ambulance.dispatch_system.optimization.service;

import com.ambulance.dispatch_system.common.entity.Shift;
import com.ambulance.dispatch_system.common.entity.ShiftSlot;
import com.ambulance.dispatch_system.common.entity.Staff;
import com.ambulance.dispatch_system.common.repository.ShiftRepository;
import com.ambulance.dispatch_system.common.repository.ShiftSlotRepository;
import com.ambulance.dispatch_system.common.repository.StaffRepository;
import com.ambulance.dispatch_system.optimization.dto.AlgorithmType;
import com.ambulance.dispatch_system.optimization.dto.ScheduleComparisonResponse;
import com.ambulance.dispatch_system.optimization.dto.ScheduleRunRequest;
import com.ambulance.dispatch_system.optimization.dto.ScheduleRunResponse;
import com.ambulance.dispatch_system.optimization.dto.ShiftDto;
import com.ambulance.dispatch_system.optimization.exception.InvalidScheduleRequestException;
import com.ambulance.dispatch_system.optimization.fitness.FitnessWeights;
import com.ambulance.dispatch_system.optimization.ga.GAParameters;
import com.ambulance.dispatch_system.optimization.ga.GeneticAlgorithmScheduler;
import com.ambulance.dispatch_system.optimization.greedy.GreedyRosterScheduler;
import com.ambulance.dispatch_system.optimization.model.RosterChromosome;
import com.ambulance.dispatch_system.optimization.model.SchedulingProblem;
import com.ambulance.dispatch_system.optimization.model.SchedulingResult;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Orchestrates the Optimization Module end to end: loads Staff and
 * ShiftSlot data from the database, runs the requested scheduling
 * algorithm(s) (see GeneticAlgorithmScheduler / GreedyRosterScheduler), and
 * persists the winning roster as Shift rows.
 */
@Service
public class SchedulingService {

    private final StaffRepository staffRepository;
    private final ShiftSlotRepository shiftSlotRepository;
    private final ShiftRepository shiftRepository;

    public SchedulingService(StaffRepository staffRepository, ShiftSlotRepository shiftSlotRepository,
                              ShiftRepository shiftRepository) {
        this.staffRepository = staffRepository;
        this.shiftSlotRepository = shiftSlotRepository;
        this.shiftRepository = shiftRepository;
    }

    /** Runs the Genetic Algorithm and, unless the request opts out, persists the resulting roster. */
    public ScheduleRunResponse run(ScheduleRunRequest request) {
        SchedulingProblem problem = buildProblem(request.weekStarting());
        FitnessWeights weights = resolveWeights(request);
        AlgorithmType algorithm = request.algorithmOrDefault();

        if (algorithm != AlgorithmType.GENETIC_ALGORITHM) {
            throw new InvalidScheduleRequestException(
                    "Use /compare to see the Greedy baseline - /run only executes the Genetic Algorithm");
        }

        SchedulingResult result = runGeneticAlgorithm(problem, request, weights);

        boolean persisted = request.shouldPersist();
        if (persisted) {
            persistRoster(result.bestChromosome(), problem);
        }

        return toResponse(result, problem, persisted);
    }

    /** Runs both the GA and the Greedy baseline against the same problem, for side-by-side evaluation. Never persists. */
    public ScheduleComparisonResponse compare(ScheduleRunRequest request) {
        SchedulingProblem problem = buildProblem(request.weekStarting());
        FitnessWeights weights = resolveWeights(request);

        SchedulingResult gaResult = runGeneticAlgorithm(problem, request, weights);
        SchedulingResult greedyResult = new GreedyRosterScheduler(problem, weights).run();

        return new ScheduleComparisonResponse(
                toResponse(gaResult, problem, false),
                toResponse(greedyResult, problem, false));
    }

    /** The persisted roster (Shift rows) for a given scheduling week. */
    public List<ShiftDto> getRoster(LocalDate weekStarting) {
        return shiftRepository.findByWeekStarting(weekStarting).stream().map(ShiftDto::fromEntity).toList();
    }

    private FitnessWeights resolveWeights(ScheduleRunRequest request) {
        return request.fitnessWeights() != null
                ? request.fitnessWeights().applyTo(FitnessWeights.defaults())
                : FitnessWeights.defaults();
    }

    private SchedulingResult runGeneticAlgorithm(SchedulingProblem problem, ScheduleRunRequest request, FitnessWeights weights) {
        GAParameters params = request.gaParameters() != null
                ? request.gaParameters().applyTo(GAParameters.defaults())
                : GAParameters.defaults();
        Random random = request.randomSeed() != null ? new Random(request.randomSeed()) : new Random();
        return new GeneticAlgorithmScheduler(problem, params, weights, random).run();
    }

    private SchedulingProblem buildProblem(LocalDate weekStarting) {
        if (weekStarting.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new InvalidScheduleRequestException("weekStarting must be a Monday");
        }

        List<ShiftSlot> shiftSlots = shiftSlotRepository.findAll();
        if (shiftSlots.isEmpty()) {
            throw new InvalidScheduleRequestException("No ShiftSlots defined - create the roster template first");
        }

        List<Staff> staff = staffRepository.findAll();
        if (staff.isEmpty()) {
            throw new InvalidScheduleRequestException("No Staff available to schedule");
        }

        return new SchedulingProblem(SchedulingProblem.expand(shiftSlots), staff, weekStarting);
    }

    /**
     * Replaces any existing roster for the week with the chromosome's assignments (understaffed/null
     * seats are simply skipped - there's no Staff to persist a Shift against). The delete and the
     * batched save are each individually transactional via Spring Data, which is enough consistency
     * for this coursework's single-writer scenario.
     */
    private void persistRoster(RosterChromosome chromosome, SchedulingProblem problem) {
        shiftRepository.deleteAll(shiftRepository.findByWeekStarting(problem.weekStarting()));

        List<ShiftSlot> slots = problem.expandedSlots();
        List<Shift> toSave = new ArrayList<>();
        for (int i = 0; i < chromosome.size(); i++) {
            Staff staff = chromosome.getGene(i);
            if (staff == null) {
                continue;
            }
            Shift shift = new Shift();
            shift.setShiftSlot(slots.get(i));
            shift.setStaff(staff);
            shift.setWeekStarting(problem.weekStarting());
            toSave.add(shift);
        }
        shiftRepository.saveAll(toSave);
    }

    private ScheduleRunResponse toResponse(SchedulingResult result, SchedulingProblem problem, boolean persisted) {
        List<ShiftDto> assignments = persisted ? getRoster(problem.weekStarting()) : previewAssignments(result, problem);
        return new ScheduleRunResponse(
                result.algorithmName(),
                result.fitnessResult(),
                result.executionTimeMillis(),
                result.generationsRun(),
                result.bestFitnessHistory(),
                assignments);
    }

    /** Builds an assignment preview straight from the chromosome, for runs that were not persisted (e.g. /compare). */
    private List<ShiftDto> previewAssignments(SchedulingResult result, SchedulingProblem problem) {
        List<ShiftSlot> slots = problem.expandedSlots();
        RosterChromosome chromosome = result.bestChromosome();
        List<ShiftDto> preview = new ArrayList<>();
        for (int i = 0; i < chromosome.size(); i++) {
            Staff staff = chromosome.getGene(i);
            if (staff == null) {
                continue;
            }
            ShiftSlot slot = slots.get(i);
            preview.add(new ShiftDto(
                    null, staff.getId(), staff.getName(), slot.getDayOfWeek(), slot.getStartTime(), slot.getEndTime(),
                    problem.weekStarting()));
        }
        return preview;
    }
}
