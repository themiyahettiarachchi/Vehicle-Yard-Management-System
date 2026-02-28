package com.vms.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Sales")
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SaleID")
    private Integer saleId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "VehicleID", nullable = false)
    private Vehicle vehicle;

    @Column(name = "CustomerName", length = 100)
    private String customerName;

    @Column(name = "CustomerPhone", length = 20)
    private String customerPhone;

    @Column(name = "SaleType", length = 50)
    private String saleType; // Regular, Export, Auction

    @Column(name = "SalePrice", precision = 15, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "SaleDate")
    private LocalDateTime saleDate;

    public Sale() {
    }

    // Getters and Setters
    public Integer getSaleId() {
        return saleId;
    }

    public void setSaleId(Integer saleId) {
        this.saleId = saleId;
    }

    // Alias getter for templates that use getId()
    public Integer getId() {
        return saleId;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    // Alias getter for templates that use getContactNumber()
    public String getContactNumber() {
        return customerPhone;
    }

    public String getSaleType() {
        return saleType;
    }

    public void setSaleType(String saleType) {
        this.saleType = saleType;
    }

    // Alias getter for templates that use getBuyerType()
    public String getBuyerType() {
        return saleType;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }

    public LocalDateTime getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(LocalDateTime saleDate) {
        this.saleDate = saleDate;
    }

    // Alias: templates may expect a status field
    public String getStatus() {
        return "Finalized";
    }

    // Alias: templates may expect an email field
    public String getEmail() {
        return null;
    }
}
