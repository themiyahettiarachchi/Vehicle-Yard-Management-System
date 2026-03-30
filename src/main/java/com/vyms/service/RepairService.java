package com.vyms.service;

import com.vyms.entity.Repair;
import com.vyms.repository.RepairRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RepairService {

    private final RepairRepository repairRepository;

    @Autowired
    public RepairService(RepairRepository repairRepository) {
        this.repairRepository = repairRepository;
    }

    public List<Repair> findAll() {
        return repairRepository.findAll();
    }

    public Optional<Repair> findById(Long id) {
        return repairRepository.findById(id);
    }

    public Repair save(Repair repair) {
        return repairRepository.save(repair);
    }

    public void deleteById(Long id) {
        repairRepository.deleteById(id);
    }
}
