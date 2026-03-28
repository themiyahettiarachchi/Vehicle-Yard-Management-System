package com.vyms.repository;

import com.vyms.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
	boolean existsByChassisNumberIgnoreCase(String chassisNumber);
	boolean existsByLicensePlateIgnoreCase(String licensePlate);
	java.util.Optional<Vehicle> findByChassisNumberIgnoreCase(String chassisNumber);
	java.util.Optional<Vehicle> findByLicensePlateIgnoreCase(String licensePlate);
}
