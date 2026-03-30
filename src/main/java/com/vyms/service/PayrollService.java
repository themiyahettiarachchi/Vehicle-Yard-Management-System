package com.vyms.service;

import com.vyms.entity.Payroll;
import com.vyms.repository.PayrollRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PayrollService {

    private final PayrollRepository payrollRepository;

    @Autowired
    public PayrollService(PayrollRepository payrollRepository) {
        this.payrollRepository = payrollRepository;
    }

    public List<Payroll> findAll() {
        return payrollRepository.findAll();
    }

    public Optional<Payroll> findById(Long id) {
        return payrollRepository.findById(id);
    }

    public Payroll save(Payroll payroll) {
        return payrollRepository.save(payroll);
    }

    public void deleteById(Long id) {
        payrollRepository.deleteById(id);
    }
}
