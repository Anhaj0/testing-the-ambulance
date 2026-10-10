package com.ambulance.dispatch_system.optimization.service;

import com.ambulance.dispatch_system.common.entity.ShiftSlot;
import com.ambulance.dispatch_system.common.repository.ShiftSlotRepository;
import com.ambulance.dispatch_system.optimization.dto.ShiftSlotDto;
import com.ambulance.dispatch_system.optimization.exception.ShiftSlotNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/** Thin CRUD service over ShiftSlot - the recurring weekly coverage template the scheduling algorithms fill. */
@Service
public class ShiftSlotService {

    private final ShiftSlotRepository shiftSlotRepository;

    public ShiftSlotService(ShiftSlotRepository shiftSlotRepository) {
        this.shiftSlotRepository = shiftSlotRepository;
    }

    public List<ShiftSlotDto> findAll() {
        return shiftSlotRepository.findAll().stream().map(ShiftSlotDto::fromEntity).toList();
    }

    public ShiftSlotDto findById(Long id) {
        return ShiftSlotDto.fromEntity(getOrThrow(id));
    }

    public ShiftSlotDto create(ShiftSlotDto request) {
        return ShiftSlotDto.fromEntity(shiftSlotRepository.save(request.toEntity()));
    }

    public ShiftSlotDto update(Long id, ShiftSlotDto request) {
        ShiftSlot slot = getOrThrow(id);
        request.applyTo(slot);
        return ShiftSlotDto.fromEntity(shiftSlotRepository.save(slot));
    }

    public void delete(Long id) {
        if (!shiftSlotRepository.existsById(id)) {
            throw new ShiftSlotNotFoundException(id);
        }
        shiftSlotRepository.deleteById(id);
    }

    private ShiftSlot getOrThrow(Long id) {
        return shiftSlotRepository.findById(id)
                .orElseThrow(() -> new ShiftSlotNotFoundException(id));
    }
}
