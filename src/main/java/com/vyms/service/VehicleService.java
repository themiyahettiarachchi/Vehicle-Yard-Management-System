package com.vyms.service;

import com.vyms.entity.Vehicle;
import com.vyms.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    @Autowired
    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    public List<Vehicle> findAll() {
        return vehicleRepository.findAll();
    }

    public Optional<Vehicle> findById(Long id) {
        return vehicleRepository.findById(id);
    }

    public Vehicle save(Vehicle vehicle) {
        return vehicleRepository.save(vehicle);
    }

    public void deleteById(Long id) {
        vehicleRepository.deleteById(id);
    }

    public boolean existsByChassisNumber(String chassisNumber) {
        return chassisNumber != null && vehicleRepository.existsByChassisNumberIgnoreCase(chassisNumber.trim());
    }

    public boolean existsByLicensePlate(String licensePlate) {
        return licensePlate != null && vehicleRepository.existsByLicensePlateIgnoreCase(licensePlate.trim());
    }

    public boolean existsOtherByChassisNumber(String chassisNumber, Long currentId) {
        if (chassisNumber == null) return false;
        return vehicleRepository.findByChassisNumberIgnoreCase(chassisNumber.trim())
                .map(v -> !v.getId().equals(currentId))
                .orElse(false);
    }

    public boolean existsOtherByLicensePlate(String licensePlate, Long currentId) {
        if (licensePlate == null) return false;
        return vehicleRepository.findByLicensePlateIgnoreCase(licensePlate.trim())
                .map(v -> !v.getId().equals(currentId))
                .orElse(false);
    }
}
