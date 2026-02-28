package com.vms.service;

import com.vms.model.ActivityFeedItem;
import com.vms.model.Sale;
import com.vms.model.Vehicle;
import com.vms.repository.SaleRepository;
import com.vms.repository.VehicleRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Service for the Sales module.
 * Now uses JPA repositories to fetch data from Microsoft SQL Server database.
 */
@Service
public class SalesService {

    private final VehicleRepository vehicleRepository;
    private final SaleRepository saleRepository;

    public SalesService(VehicleRepository vehicleRepository, SaleRepository saleRepository) {
        this.vehicleRepository = vehicleRepository;
        this.saleRepository = saleRepository;
    }

    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }

    public List<Vehicle> getAvailableVehicles() {
        return vehicleRepository.findByStatus("Available");
    }

    public List<Sale> getAllSales() {
        return saleRepository.findAllByOrderBySaleDateDesc();
    }

    public int getTotalVehicleCount() {
        return (int) vehicleRepository.count();
    }

    public int getAvailableVehicleCount() {
        return (int) vehicleRepository.countByStatus("Available");
    }

    public int getTotalSalesCount() {
        return (int) saleRepository.count();
    }

    public BigDecimal getTotalRevenue() {
        return saleRepository.getTotalRevenue();
    }

    public BigDecimal getTotalProfit() {
        BigDecimal totalRevenue = getTotalRevenue();
        List<Sale> allSales = saleRepository.findAll();
        BigDecimal totalInvestment = allSales.stream()
                .map(s -> s.getVehicle().getPurchasePrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalRevenue.subtract(totalInvestment);
    }

    // Monthly sales trend data (sample data for charts - can be enhanced with
    // actual DB queries later)
    public List<Integer> getMonthlySalesData() {
        return Arrays.asList(45, 52, 49, 61, 55, 58);
    }

    public List<String> getMonthLabels() {
        return Arrays.asList("Jan", "Feb", "Mar", "Apr", "May", "Jun");
    }

    // Revenue trend data (sample data for charts - can be enhanced with actual DB
    // queries later)
    public List<Integer> getMonthlyRevenueData() {
        return Arrays.asList(120000, 135000, 135000, 160000, 185000, 175000);
    }

    @org.springframework.transaction.annotation.Transactional
    public Sale recordSale(Integer vehicleId, String saleType, BigDecimal salePrice,
            String customerName, String customerPhone) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with ID: " + vehicleId));

        if (!"Available".equals(vehicle.getStatus())) {
            throw new RuntimeException("Vehicle is not available for sale.");
        }

        Sale sale = new Sale();
        sale.setVehicle(vehicle);
        sale.setSaleType(saleType);
        sale.setSalePrice(salePrice);
        sale.setCustomerName(customerName);
        sale.setCustomerPhone(customerPhone);
        sale.setSaleDate(java.time.LocalDateTime.now());

        Sale savedSale = saleRepository.save(sale);

        // Update vehicle status to Sold
        vehicle.setStatus("Sold");
        vehicleRepository.save(vehicle);

        return savedSale;
    }

    public List<ActivityFeedItem> getActivityFeed() {
        List<ActivityFeedItem> feed = new ArrayList<>();
        List<Sale> recentSales = saleRepository.findAllByOrderBySaleDateDesc();
        for (Sale sale : recentSales) {
            String desc = sale.getVehicle().getDisplayName() + " sold to " +
                    sale.getCustomerName() + " for Rs." + sale.getSalePrice().toBigInteger();
            feed.add(new ActivityFeedItem("Vehicle Sold", desc,
                    sale.getSaleDate() != null ? sale.getSaleDate().toLocalDate() : LocalDate.now(),
                    "#22c55e"));
        }
        return feed;
    }
}
