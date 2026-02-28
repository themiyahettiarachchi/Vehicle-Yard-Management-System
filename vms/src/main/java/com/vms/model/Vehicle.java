package com.vms.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "VehicleID")
    private Integer vehicleId;

    @Column(name = "Brand", length = 50, nullable = false)
    private String brand;

    @Column(name = "Model", length = 50, nullable = false)
    private String model;

    @Column(name = "Year", nullable = false)
    private int year;

    @Column(name = "PurchasePrice", precision = 15, scale = 2, nullable = false)
    private BigDecimal purchasePrice;

    @Column(name = "SellingPrice", precision = 15, scale = 2, nullable = false)
    private BigDecimal sellingPrice;

    @Column(name = "Status", length = 50, nullable = false)
    private String status; // Available, Under Repair, Sold

    @Column(name = "ChassisNumber", length = 50)
    private String chassisNumber;

    @Column(name = "DateAdded")
    private LocalDateTime dateAdded;

    public Vehicle() {
    }

    public Vehicle(Integer vehicleId, String brand, String model, int year,
            BigDecimal purchasePrice, BigDecimal sellingPrice, String status, String chassisNumber) {
        this.vehicleId = vehicleId;
        this.brand = brand;
        this.model = model;
        this.year = year;
        this.purchasePrice = purchasePrice;
        this.sellingPrice = sellingPrice;
        this.status = status;
        this.chassisNumber = chassisNumber;
    }

    // Getters and Setters
    public Integer getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Integer vehicleId) {
        this.vehicleId = vehicleId;
    }

    // Alias getter for templates that use getId()
    public Integer getId() {
        return vehicleId;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    // Alias getter for templates that use getMake()
    public String getMake() {
        return brand;
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

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(BigDecimal sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getChassisNumber() {
        return chassisNumber;
    }

    public void setChassisNumber(String chassisNumber) {
        this.chassisNumber = chassisNumber;
    }

    public LocalDateTime getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(LocalDateTime dateAdded) {
        this.dateAdded = dateAdded;
    }

    // Alias getter for templates that use getPlateNumber()
    public String getPlateNumber() {
        return chassisNumber;
    }

    // Alias for totalInvestment (same as purchasePrice for now)
    public BigDecimal getTotalInvestment() {
        return purchasePrice;
    }

    // Alias for repairCost (returns 0 by default - actual repair cost comes from
    // Repairs table)
    public BigDecimal getRepairCost() {
        return BigDecimal.ZERO;
    }

    public String getDisplayName() {
        return brand + " " + model + " " + year;
    }

    public String getDisplayNameWithPlate() {
        return brand + " " + model + " " + year + (chassisNumber != null ? " - " + chassisNumber : "");
    }
}
