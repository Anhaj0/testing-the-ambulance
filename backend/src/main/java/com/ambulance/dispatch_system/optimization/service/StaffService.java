package com.ambulance.dispatch_system.optimization.service;

import com.ambulance.dispatch_system.common.entity.Staff;
import com.ambulance.dispatch_system.common.repository.StaffRepository;
import com.ambulance.dispatch_system.optimization.dto.StaffDto;
import com.ambulance.dispatch_system.optimization.exception.StaffNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/** Thin CRUD service over Staff - the pool of doctors/drivers the scheduling algorithms draw from. */
@Service
public class StaffService {

    private final StaffRepository staffRepository;

    public StaffService(StaffRepository staffRepository) {
        this.staffRepository = staffRepository;
    }

    public List<StaffDto> findAll() {
        return staffRepository.findAll().stream().map(StaffDto::fromEntity).toList();
    }

    public StaffDto findById(Long id) {
        return StaffDto.fromEntity(getOrThrow(id));
    }

    public StaffDto create(StaffDto request) {
        return StaffDto.fromEntity(staffRepository.save(request.toEntity()));
    }

    public StaffDto update(Long id, StaffDto request) {
        Staff staff = getOrThrow(id);
        request.applyTo(staff);
        return StaffDto.fromEntity(staffRepository.save(staff));
    }

    public void delete(Long id) {
        if (!staffRepository.existsById(id)) {
            throw new StaffNotFoundException(id);
        }
        staffRepository.deleteById(id);
    }

    private Staff getOrThrow(Long id) {
        return staffRepository.findById(id)
                .orElseThrow(() -> new StaffNotFoundException(id));
    }
}
