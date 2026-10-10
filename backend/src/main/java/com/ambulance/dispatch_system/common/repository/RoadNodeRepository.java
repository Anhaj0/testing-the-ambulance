package com.ambulance.dispatch_system.common.repository;

import com.ambulance.dispatch_system.common.entity.RoadNode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoadNodeRepository extends JpaRepository<RoadNode, Long> {

    Optional<RoadNode> findByName(String name);
}
