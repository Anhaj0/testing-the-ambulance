package com.ambulance.dispatch_system.optimization.service;

import com.ambulance.dispatch_system.common.entity.Shift;
import com.ambulance.dispatch_system.common.entity.ShiftSlot;
import com.ambulance.dispatch_system.common.entity.Staff;
import com.ambulance.dispatch_system.common.entity.enums.StaffRole;
import com.ambulance.dispatch_system.common.repository.ShiftRepository;
import com.ambulance.dispatch_system.common.repository.ShiftSlotRepository;
import com.ambulance.dispatch_system.common.repository.StaffRepository;
import com.ambulance.dispatch_system.optimization.dto.AlgorithmType;
import com.ambulance.dispatch_system.optimization.dto.ScheduleComparisonResponse;
import com.ambulance.dispatch_system.optimization.dto.ScheduleRunRequest;
import com.ambulance.dispatch_system.optimization.dto.ScheduleRunResponse;
import com.ambulance.dispatch_system.optimization.exception.InvalidScheduleRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the orchestration layer (problem building, algorithm
 * dispatch, and roster persistence). The GA/Greedy algorithms themselves
 * are already covered by their own test classes, so these tests mock the
 * repositories and focus on SchedulingService's own responsibilities.
 */
@ExtendWith(MockitoExtension.class)
class SchedulingServiceTest {

    private static final LocalDate MONDAY = LocalDate.of(2024, 1, 1);

    @Mock
    private StaffRepository staffRepository;
    @Mock
    private ShiftSlotRepository shiftSlotRepository;
    @Mock
    private ShiftRepository shiftRepository;

    @InjectMocks
    private SchedulingService schedulingService;

    private Staff staff(String name) {
        Staff s = new Staff();
        s.setId((long) Math.abs(name.hashCode()));
        s.setName(name);
        s.setRole(StaffRole.PARAMEDIC);
        s.setMaxWeeklyHours(40);
        return s;
    }

    private ShiftSlot slot() {
        ShiftSlot slot = new ShiftSlot();
        slot.setId(1L);
        slot.setDayOfWeek(DayOfWeek.MONDAY);
        slot.setStartTime(LocalTime.of(8, 0));
        slot.setEndTime(LocalTime.of(16, 0));
        slot.setRequiredStaffCount(1);
        return slot;
    }

    @Test
    void rejectsAWeekStartingThatIsNotAMonday() {
        ScheduleRunRequest request = new ScheduleRunRequest(LocalDate.of(2024, 1, 2), AlgorithmType.GENETIC_ALGORITHM, null, null, null, false);

        assertThrows(InvalidScheduleRequestException.class, () -> schedulingService.run(request));
    }

    @Test
    void rejectsWhenNoShiftSlotsAreDefined() {
        when(shiftSlotRepository.findAll()).thenReturn(List.of());
        ScheduleRunRequest request = new ScheduleRunRequest(MONDAY, AlgorithmType.GENETIC_ALGORITHM, null, null, null, false);

        assertThrows(InvalidScheduleRequestException.class, () -> schedulingService.run(request));
    }

    @Test
    void rejectsWhenNoStaffAreAvailable() {
        when(shiftSlotRepository.findAll()).thenReturn(List.of(slot()));
        when(staffRepository.findAll()).thenReturn(List.of());
        ScheduleRunRequest request = new ScheduleRunRequest(MONDAY, AlgorithmType.GENETIC_ALGORITHM, null, null, null, false);

        assertThrows(InvalidScheduleRequestException.class, () -> schedulingService.run(request));
    }

    @Test
    void rejectsBothAlgorithmOnTheRunEndpoint() {
        when(shiftSlotRepository.findAll()).thenReturn(List.of(slot()));
        when(staffRepository.findAll()).thenReturn(List.of(staff("Alice")));
        ScheduleRunRequest request = new ScheduleRunRequest(MONDAY, AlgorithmType.BOTH, null, null, null, false);

        assertThrows(InvalidScheduleRequestException.class, () -> schedulingService.run(request));
    }

    @Test
    void rejectsGreedyAlgorithmOnTheRunEndpoint() {
        when(shiftSlotRepository.findAll()).thenReturn(List.of(slot()));
        when(staffRepository.findAll()).thenReturn(List.of(staff("Alice")));
        ScheduleRunRequest request = new ScheduleRunRequest(MONDAY, AlgorithmType.GREEDY, null, null, null, false);

        assertThrows(InvalidScheduleRequestException.class, () -> schedulingService.run(request));
    }

    @Test
    @SuppressWarnings("unchecked")
    void gaRunPersistsTheResultingRosterByDefault() {
        when(shiftSlotRepository.findAll()).thenReturn(List.of(slot()));
        when(staffRepository.findAll()).thenReturn(List.of(staff("Alice")));
        when(shiftRepository.findByWeekStarting(MONDAY)).thenReturn(List.of());

        ScheduleRunRequest request = new ScheduleRunRequest(MONDAY, AlgorithmType.GENETIC_ALGORITHM, null, null, null, null);
        ScheduleRunResponse response = schedulingService.run(request);

        assertEquals("Genetic Algorithm", response.algorithmName());

        ArgumentCaptor<List<Shift>> captor = ArgumentCaptor.forClass(List.class);
        verify(shiftRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals("Alice", captor.getValue().get(0).getStaff().getName());
    }

    @Test
    void previewRunDoesNotTouchTheRepositoryWhenPersistIsFalse() {
        when(shiftSlotRepository.findAll()).thenReturn(List.of(slot()));
        when(staffRepository.findAll()).thenReturn(List.of(staff("Alice")));

        ScheduleRunRequest request = new ScheduleRunRequest(MONDAY, AlgorithmType.GENETIC_ALGORITHM, null, null, null, false);
        ScheduleRunResponse response = schedulingService.run(request);

        assertEquals(1, response.assignments().size());
        assertEquals("Alice", response.assignments().get(0).staffName());
        verify(shiftRepository, never()).saveAll(anyList());
        verify(shiftRepository, never()).deleteAll(anyList());
    }

    @Test
    void compareRunsBothAlgorithmsWithoutPersisting() {
        when(shiftSlotRepository.findAll()).thenReturn(List.of(slot()));
        when(staffRepository.findAll()).thenReturn(List.of(staff("Alice"), staff("Bob")));

        ScheduleRunRequest request = new ScheduleRunRequest(MONDAY, null, 42L, null, null, null);
        ScheduleComparisonResponse response = schedulingService.compare(request);

        assertEquals("Genetic Algorithm", response.geneticAlgorithm().algorithmName());
        assertEquals("Greedy", response.greedy().algorithmName());
        verify(shiftRepository, never()).saveAll(anyList());
        verify(shiftRepository, never()).deleteAll(anyList());
    }
}
