package com.vms.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Sale {
    private Long id;
    private Vehicle vehicle;
    private String buyerType; // Regular Customer, Export, Auction
    private String customerName;
    private String contactNumber;
    private String email;
    private BigDecimal salePrice;
    private LocalDate saleDate;
    private String status; // Finalized, Pending

    public Sale() {
    }

    public Sale(Long id, Vehicle vehicle, String buyerType, String customerName,
            String contactNumber, String email, BigDecimal salePrice,
            LocalDate saleDate, String status) {
        this.id = id;
        this.vehicle = vehicle;
        this.buyerType = buyerType;
        this.customerName = customerName;
        this.contactNumber = contactNumber;
        this.email = email;
        this.salePrice = salePrice;
        this.saleDate = saleDate;
        this.status = status;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public String getBuyerType() {
        return buyerType;
    }

    public void setBuyerType(String buyerType) {
        this.buyerType = buyerType;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }

    public LocalDate getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(LocalDate saleDate) {
        this.saleDate = saleDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
