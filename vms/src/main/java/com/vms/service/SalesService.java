package com.vms.service;

import com.vms.model.ActivityFeedItem;
import com.vms.model.Sale;
import com.vms.model.Vehicle;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service providing sample data for the Sales module frontend.
 * This will be replaced with actual database calls when the backend is
 * implemented.
 */
@Service
public class SalesService {

    private final List<Vehicle> vehicles = new ArrayList<>();
    private final List<Sale> sales = new ArrayList<>();

    public SalesService() {
        initSampleData();
    }

    private void initSampleData() {
        // Sample vehicles
        Vehicle v1 = new Vehicle(1L, "Toyota", "Camry", 2022, "ABC-1234",
                new BigDecimal("25000"), new BigDecimal("4150"), new BigDecimal("29150"), "SOLD");
        Vehicle v2 = new Vehicle(2L, "Honda", "Accord", 2023, "XYZ-5678",
                new BigDecimal("27000"), new BigDecimal("2200"), new BigDecimal("29200"), "AVAILABLE");
        Vehicle v3 = new Vehicle(3L, "BMW", "X5", 2021, "DEF-9012",
                new BigDecimal("45000"), new BigDecimal("2500"), new BigDecimal("47500"), "AVAILABLE");

        vehicles.addAll(Arrays.asList(v1, v2, v3));

        // Sample sales
        Sale s1 = new Sale(1L, v1, "Regular Customer", "John Smith",
                "+1-555-0101", "john.smith@email.com", new BigDecimal("27000"),
                LocalDate.of(2026, 2, 10), "Finalized");

        sales.add(s1);
    }

    public List<Vehicle> getAllVehicles() {
        return vehicles;
    }

    public List<Vehicle> getAvailableVehicles() {
        return vehicles.stream()
                .filter(v -> "AVAILABLE".equals(v.getStatus()))
                .collect(Collectors.toList());
    }

    public List<Sale> getAllSales() {
        return sales;
    }

    public int getTotalVehicleCount() {
        return vehicles.size();
    }

    public int getAvailableVehicleCount() {
        return (int) vehicles.stream().filter(v -> "AVAILABLE".equals(v.getStatus())).count();
    }

    public int getTotalSalesCount() {
        return sales.size();
    }

    public BigDecimal getTotalRevenue() {
        return sales.stream()
                .map(Sale::getSalePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalProfit() {
        BigDecimal totalRevenue = getTotalRevenue();
        BigDecimal totalInvestment = sales.stream()
                .map(s -> s.getVehicle().getTotalInvestment())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalRevenue.subtract(totalInvestment);
    }

    // Monthly sales trend data (sample data for charts)
    public List<Integer> getMonthlySalesData() {
        return Arrays.asList(45, 52, 49, 61, 55, 58);
    }

    public List<String> getMonthLabels() {
        return Arrays.asList("Jan", "Feb", "Mar", "Apr", "May", "Jun");
    }

    // Revenue trend data (sample data for charts)
    public List<Integer> getMonthlyRevenueData() {
        return Arrays.asList(120000, 135000, 135000, 160000, 185000, 175000);
    }

    public List<ActivityFeedItem> getActivityFeed() {
        List<ActivityFeedItem> feed = new ArrayList<>();
        for (Sale sale : sales) {
            String desc = sale.getVehicle().getDisplayName() + " sold to " +
                    sale.getCustomerName() + " for $" + sale.getSalePrice().toBigInteger();
            feed.add(new ActivityFeedItem("Vehicle Sold", desc, sale.getSaleDate(), "#22c55e"));
        }
        return feed;
    }
}
