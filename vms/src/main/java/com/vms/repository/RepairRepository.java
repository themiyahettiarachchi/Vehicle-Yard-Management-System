package com.vms.repository;

import com.vms.model.Repair;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepairRepository extends JpaRepository<Repair, Integer> {
    List<Repair> findByVehicle_VehicleId(Integer vehicleId);

    List<Repair> findByRepairStatus(String repairStatus);
}
