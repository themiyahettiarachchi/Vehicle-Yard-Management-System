package com.vyms.controller;

import com.vyms.entity.BuyerType;
import com.vyms.entity.Sale;
import com.vyms.entity.Vehicle;
import com.vyms.service.PdfReportService;
import com.vyms.service.SaleService;
import com.vyms.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/sales-dashboard")
public class SalesDashboardController {

    private final SaleService saleService;
    private final VehicleService vehicleService;

    // Validation patterns
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[\\d\\s+()-]{5,20}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^.+@.+\\..+$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^.{2,100}$");

    private boolean isValidSaleInputs(String buyerType, String customerName, String contactNumber, String email) {
        if ("REGULAR_CUSTOMER".equals(buyerType)) {
            if (customerName == null || !NAME_PATTERN.matcher(customerName.trim()).matches()) {
                System.out.println("Validation failed on Customer Name: " + customerName);
                return false;
            }
            if (contactNumber == null || !PHONE_PATTERN.matcher(contactNumber.trim()).matches()) {
                System.out.println("Validation failed on Contact Number: " + contactNumber);
                return false;
            }
            if (email != null && !email.trim().isEmpty() && !EMAIL_PATTERN.matcher(email.trim()).matches()) {
                System.out.println("Validation failed on Email: " + email);
                return false;
            }
        }
        return true;
    }

    private final PdfReportService pdfReportService;

    @Autowired
    public SalesDashboardController(SaleService saleService, VehicleService vehicleService,
                                    PdfReportService pdfReportService) {
        this.saleService = saleService;
        this.vehicleService = vehicleService;
        this.pdfReportService = pdfReportService;
    }

    // Main dashboard
    @GetMapping
    public String dashboard(Model model) {
        List<Sale> sales = saleService.findAll();
        List<Vehicle> vehicles = vehicleService.findAll();

        BigDecimal totalRevenue = sales.stream()
                .map(s -> s.getSalePrice() != null ? s.getSalePrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long availableVehicles = vehicles.stream()
                .filter(v -> !"SOLD".equalsIgnoreCase(v.getStatus()))
                .count();

        model.addAttribute("totalSales", sales.size());
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("availableVehicles", availableVehicles);

        // Chart 1: Buyer Demographics
        long countRegular = sales.stream().filter(s -> s.getBuyerType() == BuyerType.REGULAR_CUSTOMER).count();
        long countExport = sales.stream().filter(s -> s.getBuyerType() == BuyerType.EXPORT).count();
        long countAuction = sales.stream().filter(s -> s.getBuyerType() == BuyerType.AUCTION).count();
        model.addAttribute("countRegular", countRegular);
        model.addAttribute("countExport", countExport);
        model.addAttribute("countAuction", countAuction);

        // Chart 2: Recent Transactions Profitability (Last 10 sales)
        List<Sale> recentSales = sales.stream()
                .sorted((s1, s2) -> Long.compare(s2.getId(), s1.getId())) // assuming higher ID is newer
                .limit(10)
                .collect(Collectors.toList());
        
        List<String> recentLabels = recentSales.stream()
                .map(s -> s.getVehicle() != null ? s.getVehicle().getVehicleModel() : "Unknown")
                .collect(Collectors.toList());
        List<BigDecimal> recentPrices = recentSales.stream()
                .map(s -> s.getSalePrice() != null ? s.getSalePrice() : BigDecimal.ZERO)
                .collect(Collectors.toList());
        List<BigDecimal> recentCosts = recentSales.stream()
                .map(s -> s.getTotalCost() != null ? s.getTotalCost() : BigDecimal.ZERO)
                .collect(Collectors.toList());

        model.addAttribute("recentLabels", recentLabels);
        model.addAttribute("recentPrices", recentPrices);
        model.addAttribute("recentCosts", recentCosts);

        return "sales";
    }

    // Sales Management page
    @GetMapping("/sales")
    public String salesManagement(Model model) {
        List<Sale> sales = saleService.findAll();
        List<Vehicle> vehicles = vehicleService.findAll();

        // Only show unsold vehicles for the dropdown
        List<Vehicle> unsoldVehicles = vehicles.stream()
                .filter(v -> !"SOLD".equalsIgnoreCase(v.getStatus()))
                .collect(Collectors.toList());

        BigDecimal totalRevenue = sales.stream()
                .map(s -> s.getSalePrice() != null ? s.getSalePrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("sales", sales);
        model.addAttribute("vehicles", unsoldVehicles);
        model.addAttribute("allVehicles", vehicles);
        model.addAttribute("buyerTypes", BuyerType.values());
        model.addAttribute("totalSales", sales.size());
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("availableVehicles", unsoldVehicles.size());
        return "sales/sales-management";
    }

    // Record a new sale
    @PostMapping("/sales/record")
    public String recordSale(
            @RequestParam("vehicleId") Long vehicleId,
            @RequestParam("salePrice") BigDecimal salePrice,
            @RequestParam("buyerType") String buyerType,
            @RequestParam(name = "customerName", required = false) String customerName,
            @RequestParam(name = "contactNumber", required = false) String contactNumber,
            @RequestParam(name = "email", required = false) String email,
            @RequestParam(name = "companyName", required = false) String companyName,
            @RequestParam(name = "exportCountry", required = false) String exportCountry,
            @RequestParam(name = "auctionHouseName", required = false) String auctionHouseName,
            @RequestParam(name = "location", required = false) String location,
            @RequestParam(name = "lotNumber", required = false) String lotNumber) {

        if (!isValidSaleInputs(buyerType, customerName, contactNumber, email)) {
            return "redirect:/sales-dashboard/sales";
        }

        Optional<Vehicle> vehicleOpt = vehicleService.findById(vehicleId);
        if (vehicleOpt.isPresent()) {
            Vehicle v = vehicleOpt.get();

            // Snapshot the cost at sale time
            BigDecimal purchase = v.getPurchasePrice() != null ? v.getPurchasePrice() : BigDecimal.ZERO;
            BigDecimal repair = v.getRepairCost() != null ? v.getRepairCost() : BigDecimal.ZERO;
            BigDecimal snapshotCost = purchase.add(repair);

            Sale sale = new Sale();
            sale.setVehicle(v);
            sale.setSalePrice(salePrice);
            sale.setBuyerType(BuyerType.valueOf(buyerType));
            sale.setSaleDate(LocalDate.now());
            sale.setSaleStatus("FINALIZED");
            sale.setTotalCost(snapshotCost);

            if ("REGULAR_CUSTOMER".equals(buyerType)) {
                sale.setCustomerName(customerName);
                sale.setContactNumber(contactNumber);
                sale.setEmail(email);
            } else if ("EXPORT".equals(buyerType)) {
                sale.setCompanyName(companyName);
                sale.setExportCountry(exportCountry);
                sale.setEmail(email);
            } else if ("AUCTION".equals(buyerType)) {
                sale.setAuctionHouseName(auctionHouseName);
                sale.setLocation(location);
                sale.setLotNumber(lotNumber);
            }

            // Update vehicle status and salePrice
            v.setStatus("SOLD");
            v.setSalePrice(salePrice);
            vehicleService.save(v);

            saleService.save(sale);
        }
        return "redirect:/sales-dashboard/sales";
    }

    // Edit an existing sale
    @PostMapping("/sales/edit/{id}")
    public String editSale(@PathVariable("id") Long id,
            @RequestParam("salePrice") BigDecimal salePrice,
            @RequestParam("buyerType") String buyerType,
            @RequestParam(name = "customerName", required = false) String customerName,
            @RequestParam(name = "contactNumber", required = false) String contactNumber,
            @RequestParam(name = "email", required = false) String email,
            @RequestParam(name = "companyName", required = false) String companyName,
            @RequestParam(name = "exportCountry", required = false) String exportCountry,
            @RequestParam(name = "auctionHouseName", required = false) String auctionHouseName,
            @RequestParam(name = "location", required = false) String location,
            @RequestParam(name = "lotNumber", required = false) String lotNumber,
            @RequestParam(name = "saleStatus", required = false) String saleStatus) {
            
        if (!isValidSaleInputs(buyerType, customerName,
                contactNumber != null ? contactNumber : "",
                email)) {
            return "redirect:/sales-dashboard/sales";
        }
        Optional<Sale> saleOpt = saleService.findById(id);
        saleOpt.ifPresent(s -> {
            s.setSalePrice(salePrice);
            s.setBuyerType(BuyerType.valueOf(buyerType));
            
            // Clear existing data to avoid crossover
            s.setCustomerName(null);
            s.setContactNumber(null);
            s.setEmail(null);
            s.setCompanyName(null);
            s.setExportCountry(null);
            s.setAuctionHouseName(null);
            s.setLocation(null);
            s.setLotNumber(null);

            if ("REGULAR_CUSTOMER".equals(buyerType)) {
                s.setCustomerName(customerName);
                s.setContactNumber(contactNumber);
                if (email != null) s.setEmail(email);
            } else if ("EXPORT".equals(buyerType)) {
                s.setCompanyName(companyName);
                s.setExportCountry(exportCountry);
                if (email != null) s.setEmail(email);
            } else if ("AUCTION".equals(buyerType)) {
                s.setAuctionHouseName(auctionHouseName);
                s.setLocation(location);
                s.setLotNumber(lotNumber);
            }

            if (saleStatus != null)
                s.setSaleStatus(saleStatus);
            if (s.getVehicle() != null) {
                s.getVehicle().setSalePrice(salePrice);
                vehicleService.save(s.getVehicle());
            }
            saleService.save(s);
        });
        return "redirect:/sales-dashboard/sales";
    }

    // Invoice page for a specific sale
    @GetMapping("/sales/invoice/{id}")
    public String invoice(@PathVariable("id") Long id, Model model) {
        Optional<Sale> saleOpt = saleService.findById(id);
        if (saleOpt.isPresent()) {
            Sale sale = saleOpt.get();
            model.addAttribute("sale", sale);
            return "sales/invoice";
        }
        return "redirect:/sales-dashboard/sales";
    }

    // Inventory (read-only view)
    @GetMapping("/inventory")
    public String inventory(Model model) {
        model.addAttribute("vehicles", vehicleService.findAll());
        return "sales/inventory-view";
    }

    // API endpoint: get vehicle data as JSON for the modal profit calculation
    @GetMapping("/api/vehicle/{id}")
    @ResponseBody
    public String vehicleData(@PathVariable("id") Long id) {
        Optional<Vehicle> opt = vehicleService.findById(id);
        if (opt.isPresent()) {
            Vehicle v = opt.get();
            BigDecimal purchase = v.getPurchasePrice() != null ? v.getPurchasePrice() : BigDecimal.ZERO;
            BigDecimal repair = v.getRepairCost() != null ? v.getRepairCost() : BigDecimal.ZERO;
            BigDecimal total = purchase.add(repair);
            return "{\"purchasePrice\":" + purchase + ",\"repairCost\":" + repair + ",\"totalInvestment\":" + total
                    + "}";
        }
        return "{}";
    }

    // =========================================================================
    // Download: Sales Report PDF
    // =========================================================================
    @GetMapping("/sales/download")
    public ResponseEntity<byte[]> downloadSalesReport() {
        List<Sale> sales = saleService.findAll();
        byte[] pdf = pdfReportService.salesReport(sales);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"sales-report.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
