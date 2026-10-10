package com.ambulance.dispatch_system.optimization.service;

import com.ambulance.dispatch_system.common.entity.Staff;
import com.ambulance.dispatch_system.common.entity.enums.StaffRole;
import com.ambulance.dispatch_system.common.repository.StaffRepository;
import com.ambulance.dispatch_system.optimization.dto.StaffDto;
import com.ambulance.dispatch_system.optimization.exception.StaffNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffServiceTest {

    @Mock
    private StaffRepository staffRepository;

    @InjectMocks
    private StaffService staffService;

    @Test
    void createSavesAMappedEntityAndReturnsItsDto() {
        StaffDto request = new StaffDto(null, "Alice", StaffRole.PARAMEDIC, Set.of(), 40);
        Staff saved = new Staff();
        saved.setId(1L);
        saved.setName("Alice");
        saved.setRole(StaffRole.PARAMEDIC);
        saved.setMaxWeeklyHours(40);
        when(staffRepository.save(any(Staff.class))).thenReturn(saved);

        StaffDto result = staffService.create(request);

        assertEquals(1L, result.id());
        assertEquals("Alice", result.name());
    }

    @Test
    void findByIdThrowsNotFoundWhenMissing() {
        when(staffRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(StaffNotFoundException.class, () -> staffService.findById(99L));
    }

    @Test
    void deleteThrowsNotFoundWhenMissingAndNeverCallsDeleteById() {
        when(staffRepository.existsById(99L)).thenReturn(false);

        assertThrows(StaffNotFoundException.class, () -> staffService.delete(99L));
        verify(staffRepository, never()).deleteById(any());
    }
}
