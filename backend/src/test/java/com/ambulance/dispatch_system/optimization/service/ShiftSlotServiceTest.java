package com.ambulance.dispatch_system.optimization.service;

import com.ambulance.dispatch_system.common.entity.ShiftSlot;
import com.ambulance.dispatch_system.common.repository.ShiftSlotRepository;
import com.ambulance.dispatch_system.optimization.dto.ShiftSlotDto;
import com.ambulance.dispatch_system.optimization.exception.ShiftSlotNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShiftSlotServiceTest {

    @Mock
    private ShiftSlotRepository shiftSlotRepository;

    @InjectMocks
    private ShiftSlotService shiftSlotService;

    @Test
    void createSavesAMappedEntityAndReturnsItsDto() {
        ShiftSlotDto request = new ShiftSlotDto(null, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(16, 0), null, 1);
        ShiftSlot saved = new ShiftSlot();
        saved.setId(1L);
        saved.setDayOfWeek(DayOfWeek.MONDAY);
        saved.setStartTime(LocalTime.of(8, 0));
        saved.setEndTime(LocalTime.of(16, 0));
        saved.setRequiredStaffCount(1);
        when(shiftSlotRepository.save(any(ShiftSlot.class))).thenReturn(saved);

        ShiftSlotDto result = shiftSlotService.create(request);

        assertEquals(1L, result.id());
        assertEquals(DayOfWeek.MONDAY, result.dayOfWeek());
    }

    @Test
    void findByIdThrowsNotFoundWhenMissing() {
        when(shiftSlotRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ShiftSlotNotFoundException.class, () -> shiftSlotService.findById(99L));
    }

    @Test
    void deleteThrowsNotFoundWhenMissingAndNeverCallsDeleteById() {
        when(shiftSlotRepository.existsById(99L)).thenReturn(false);

        assertThrows(ShiftSlotNotFoundException.class, () -> shiftSlotService.delete(99L));
        verify(shiftSlotRepository, never()).deleteById(any());
    }
}
