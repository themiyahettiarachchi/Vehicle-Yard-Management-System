package com.vms.repository;

import com.vms.model.Salary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalaryRepository extends JpaRepository<Salary, Integer> {
    List<Salary> findByEmployee_EmployeeId(Integer employeeId);

    List<Salary> findBySalaryMonthAndSalaryYear(String month, Integer year);
}
