package com.vms.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Salary")
public class Salary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SalaryID")
    private Integer salaryId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "EmployeeID", nullable = false)
    private Employee employee;

    @Column(name = "SalaryMonth", length = 20, nullable = false)
    private String salaryMonth;

    @Column(name = "SalaryYear", nullable = false)
    private Integer salaryYear;

    @Column(name = "BasicSalary", precision = 12, scale = 2)
    private BigDecimal basicSalary;

    @Column(name = "Bonus", precision = 12, scale = 2)
    private BigDecimal bonus;

    @Column(name = "Deductions", precision = 12, scale = 2)
    private BigDecimal deductions;

    // NetSalary is a computed column in the DB (BasicSalary + Bonus - Deductions)
    @Column(name = "NetSalary", insertable = false, updatable = false)
    private BigDecimal netSalary;

    @Column(name = "PaidDate")
    private LocalDateTime paidDate;

    public Salary() {
    }

    // Getters and Setters
    public Integer getSalaryId() {
        return salaryId;
    }

    public void setSalaryId(Integer salaryId) {
        this.salaryId = salaryId;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public String getSalaryMonth() {
        return salaryMonth;
    }

    public void setSalaryMonth(String salaryMonth) {
        this.salaryMonth = salaryMonth;
    }

    public Integer getSalaryYear() {
        return salaryYear;
    }

    public void setSalaryYear(Integer salaryYear) {
        this.salaryYear = salaryYear;
    }

    public BigDecimal getBasicSalary() {
        return basicSalary;
    }

    public void setBasicSalary(BigDecimal basicSalary) {
        this.basicSalary = basicSalary;
    }

    public BigDecimal getBonus() {
        return bonus;
    }

    public void setBonus(BigDecimal bonus) {
        this.bonus = bonus;
    }

    public BigDecimal getDeductions() {
        return deductions;
    }

    public void setDeductions(BigDecimal deductions) {
        this.deductions = deductions;
    }

    public BigDecimal getNetSalary() {
        return netSalary;
    }

    public void setNetSalary(BigDecimal netSalary) {
        this.netSalary = netSalary;
    }

    public LocalDateTime getPaidDate() {
        return paidDate;
    }

    public void setPaidDate(LocalDateTime paidDate) {
        this.paidDate = paidDate;
    }
}
