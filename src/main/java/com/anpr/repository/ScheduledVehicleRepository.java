package com.anpr.repository;

import com.anpr.entity.ScheduledVehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScheduledVehicleRepository extends JpaRepository<ScheduledVehicle, Long> {
}
