package com.vms.model;

import java.math.BigDecimal;

public class Vehicle {
    private Long id;
    private String make;
    private String model;
    private int year;
    private String plateNumber;
    private BigDecimal purchasePrice;
    private BigDecimal repairCost;
    private BigDecimal totalInvestment;
    private String status; // AVAILABLE, SOLD

    public Vehicle() {
    }

    public Vehicle(Long id, String make, String model, int year, String plateNumber,
            BigDecimal purchasePrice, BigDecimal repairCost, BigDecimal totalInvestment, String status) {
        this.id = id;
        this.make = make;
        this.model = model;
        this.year = year;
        this.plateNumber = plateNumber;
        this.purchasePrice = purchasePrice;
        this.repairCost = repairCost;
        this.totalInvestment = totalInvestment;
        this.status = status;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public BigDecimal getRepairCost() {
        return repairCost;
    }

    public void setRepairCost(BigDecimal repairCost) {
        this.repairCost = repairCost;
    }

    public BigDecimal getTotalInvestment() {
        return totalInvestment;
    }

    public void setTotalInvestment(BigDecimal totalInvestment) {
        this.totalInvestment = totalInvestment;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDisplayName() {
        return make + " " + model + " " + year;
    }

    public String getDisplayNameWithPlate() {
        return make + " " + model + " " + year + " - " + plateNumber;
    }
}
