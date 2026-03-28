package com.vyms.repository;

import com.vyms.entity.Payroll;
import com.vyms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollRepository extends JpaRepository<Payroll, Long> {

    /**
     * All payroll records for a specific month+year (used for the salary overview
     * table)
     */
    List<Payroll> findByMonthAndYear(int month, int year);

    /** Check if payroll was already generated for a specific worker+month+year */
    Optional<Payroll> findByUserAndMonthAndYear(User user, int month, int year);
}
