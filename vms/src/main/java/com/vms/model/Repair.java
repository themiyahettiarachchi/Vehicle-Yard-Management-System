package com.vms.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "Repairs")
public class Repair {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RepairID")
    private Integer repairId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "VehicleID", nullable = false)
    private Vehicle vehicle;

    @Column(name = "Description", length = 200)
    private String description;

    @Column(name = "RepairCost", precision = 12, scale = 2)
    private BigDecimal repairCost;

    @Column(name = "RepairDate")
    private LocalDate repairDate;

    @Column(name = "RepairStatus", length = 50)
    private String repairStatus; // Pending, In Progress, Completed

    public Repair() {
    }

    // Getters and Setters
    public Integer getRepairId() {
        return repairId;
    }

    public void setRepairId(Integer repairId) {
        this.repairId = repairId;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getRepairCost() {
        return repairCost;
    }

    public void setRepairCost(BigDecimal repairCost) {
        this.repairCost = repairCost;
    }

    public LocalDate getRepairDate() {
        return repairDate;
    }

    public void setRepairDate(LocalDate repairDate) {
        this.repairDate = repairDate;
    }

    public String getRepairStatus() {
        return repairStatus;
    }

    public void setRepairStatus(String repairStatus) {
        this.repairStatus = repairStatus;
    }
}
