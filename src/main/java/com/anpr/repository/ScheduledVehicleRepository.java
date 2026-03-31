package com.anpr.repository;

import com.anpr.entity.ScheduledVehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ScheduledVehicleRepository extends JpaRepository<ScheduledVehicle, Long> {

	List<ScheduledVehicle> findByUpliftDate(LocalDate upliftDate);

	List<ScheduledVehicle> findByTruckRegNoIgnoreCaseOrTrailorNoIgnoreCase(String truckRegNo, String trailorNo);
}
