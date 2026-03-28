package com.vyms.controller;

import com.vyms.entity.Vehicle;
import com.vyms.service.PdfReportService;
import com.vyms.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/inventory")
public class InventoryController {

    private final VehicleService vehicleService;
    private final PdfReportService pdfReportService;

    @Value("${app.upload-dir}")
    private String uploadDir;

    @Autowired
    public InventoryController(VehicleService vehicleService, PdfReportService pdfReportService) {
        this.vehicleService = vehicleService;
        this.pdfReportService = pdfReportService;
    }

    private static final Set<String> KNOWN_BRANDS = new LinkedHashSet<>(Arrays.asList(
        // Japanese brands
        "Toyota", "Honda", "Nissan", "Mazda", "Subaru", "Mitsubishi", "Suzuki",
        "Lexus", "Daihatsu", "Isuzu", "Acura", "Infiniti",
        // Other common brands
        "Jeep", "Ford", "BMW", "Mercedes", "Chevrolet", "Volkswagen",
        "Hyundai", "Kia", "Audi", "Volvo", "Jaguar", "Land", "Range",
        "Peugeot", "Renault", "Fiat", "Ferrari", "Porsche"
    ));

    private String extractBrand(String vehicleModel) {
        if (vehicleModel == null || vehicleModel.isBlank()) return "Other";
        String first = vehicleModel.trim().split("[\\s-]+")[0];
        for (String brand : KNOWN_BRANDS) {
            if (brand.equalsIgnoreCase(first)) return brand;
        }
        // Capitalize and return the first word anyway
        return first.substring(0, 1).toUpperCase() + first.substring(1).toLowerCase();
    }

    @GetMapping
    public String dashboard(Model model) {
        List<Vehicle> vehicles = vehicleService.findAll();
        
        long totalVehicles = vehicles.size();
        long soldCount = vehicles.stream().filter(v -> "SOLD".equalsIgnoreCase(v.getStatus())).count();
        long unsoldCount = totalVehicles - soldCount;
        
        // Sum of all purchase & repair for current stock 
        BigDecimal totalInvestment = vehicles.stream()
            .filter(v -> !"SOLD".equalsIgnoreCase(v.getStatus()))
            .map(v -> {
                BigDecimal p = v.getPurchasePrice() != null ? v.getPurchasePrice() : BigDecimal.ZERO;
                BigDecimal r = v.getRepairCost() != null ? v.getRepairCost() : BigDecimal.ZERO;
                return p.add(r);
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Chart 2: Brand Distribution (% of all vehicles by brand)
        Map<String, Long> brandCounts = vehicles.stream()
            .collect(Collectors.groupingBy(v -> extractBrand(v.getVehicleModel()), Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        model.addAttribute("vehicles", vehicles);
        model.addAttribute("totalVehicles", totalVehicles);
        model.addAttribute("soldCount", soldCount);
        model.addAttribute("unsoldCount", unsoldCount);
        model.addAttribute("totalInvestment", totalInvestment);

        model.addAttribute("brandLabels", brandCounts.keySet());
        model.addAttribute("brandData", brandCounts.values());

        return "inventory";
    }

    @GetMapping("/vehicles")
    public String listVehicles(Model model) {
        model.addAttribute("vehicles", vehicleService.findAll());
        return "inventory/vehicles";
    }

    // ── Reports ──────────────────────────────────────────────────────────────

    @GetMapping("/reports")
    public String reportsPage(Model model) {
        List<Vehicle> vehicles = vehicleService.findAll();
        long totalVehicles = vehicles.size();
        long soldCount   = vehicles.stream().filter(v -> "SOLD".equalsIgnoreCase(v.getStatus())).count();
        long unsoldCount = totalVehicles - soldCount;

        BigDecimal totalInvestment = vehicles.stream()
            .filter(v -> !"SOLD".equalsIgnoreCase(v.getStatus()))
            .map(v -> {
                BigDecimal p = v.getPurchasePrice() != null ? v.getPurchasePrice() : BigDecimal.ZERO;
                BigDecimal r = v.getRepairCost()    != null ? v.getRepairCost()    : BigDecimal.ZERO;
                return p.add(r);
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal potentialRevenue = vehicles.stream()
            .filter(v -> !"SOLD".equalsIgnoreCase(v.getStatus()))
            .map(v -> v.getSalePrice() != null ? v.getSalePrice() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal soldRevenue = vehicles.stream()
            .filter(v -> "SOLD".equalsIgnoreCase(v.getStatus()))
            .map(v -> v.getSalePrice() != null ? v.getSalePrice() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("vehicles", vehicles);
        model.addAttribute("totalVehicles", totalVehicles);
        model.addAttribute("soldCount", soldCount);
        model.addAttribute("unsoldCount", unsoldCount);
        model.addAttribute("totalInvestment", totalInvestment);
        model.addAttribute("potentialRevenue", potentialRevenue);
        model.addAttribute("soldRevenue", soldRevenue);
        return "inventory/reports";
    }

    @GetMapping("/reports/download")
    public ResponseEntity<byte[]> downloadReport() {
        List<Vehicle> vehicles = vehicleService.findAll();
        byte[] pdf = pdfReportService.inventoryStockReport(vehicles);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=inventory-stock-report.pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }

    // Add vehicle - POST
    @PostMapping("/vehicles/add")
    public String addVehicle(
            @RequestParam("chassisNumber") String chassisNumber,
            @RequestParam("licensePlate") String licensePlate,
            @RequestParam("vehicleModel") String vehicleModel,
            @RequestParam(name = "purchasePrice", required = false) BigDecimal purchasePrice,
            @RequestParam(name = "imageFile", required = false) MultipartFile imageFile,
            RedirectAttributes ra) throws IOException {

        String chassis = chassisNumber != null ? chassisNumber.trim().toUpperCase() : "";
        String plate = licensePlate != null ? licensePlate.trim().toUpperCase() : "";

        if (vehicleService.existsByChassisNumber(chassis)) {
            ra.addFlashAttribute("uploadError", "A vehicle with this chassis number already exists.");
            return "redirect:/inventory/vehicles";
        }
        if (vehicleService.existsByLicensePlate(plate)) {
            ra.addFlashAttribute("uploadError", "A vehicle with this license plate already exists.");
            return "redirect:/inventory/vehicles";
        }

        Vehicle v = new Vehicle();
        v.setChassisNumber(chassis);
        v.setLicensePlate(plate);
        v.setVehicleModel(vehicleModel);
        v.setPurchasePrice(purchasePrice != null ? purchasePrice : BigDecimal.ZERO);
        v.setRepairCost(BigDecimal.ZERO);
        v.setStatus("UNSOLD");

        if (imageFile != null && !imageFile.isEmpty()) {
            String imagePath = saveImage(imageFile);
            v.setImagePath(imagePath);
        }

        vehicleService.save(v);
        ra.addFlashAttribute("successMsg", "Vehicle added successfully.");
        return "redirect:/inventory/vehicles";
    }

    // Edit vehicle form - GET
    @GetMapping("/vehicles/edit/{id}")
    public String editVehicleForm(@PathVariable("id") Long id, Model model) {
        Optional<Vehicle> vehicleOpt = vehicleService.findById(id);
        if (vehicleOpt.isPresent()) {
            model.addAttribute("vehicle", vehicleOpt.get());
            return "inventory/vehicle-edit";
        }
        return "redirect:/inventory/vehicles";
    }

    // Edit vehicle - POST (supports image upload)
    @PostMapping("/vehicles/edit/{id}")
    public String updateVehicle(
            @PathVariable("id") Long id,
            @RequestParam("chassisNumber") String chassisNumber,
            @RequestParam("licensePlate") String licensePlate,
            @RequestParam("vehicleModel") String vehicleModel,
            @RequestParam(name = "purchasePrice", required = false) BigDecimal purchasePrice,
            @RequestParam(name = "repairCost", required = false) BigDecimal repairCost,
            @RequestParam(name = "salePrice", required = false) BigDecimal salePrice,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "imageFile", required = false) MultipartFile imageFile,
            RedirectAttributes ra) throws IOException {

        String chassis = chassisNumber != null ? chassisNumber.trim().toUpperCase() : "";
        String plate = licensePlate != null ? licensePlate.trim().toUpperCase() : "";

        if (vehicleService.existsOtherByChassisNumber(chassis, id)) {
            ra.addFlashAttribute("uploadError", "Another vehicle already has this chassis number.");
            return "redirect:/inventory/vehicles";
        }
        if (vehicleService.existsOtherByLicensePlate(plate, id)) {
            ra.addFlashAttribute("uploadError", "Another vehicle already has this license plate.");
            return "redirect:/inventory/vehicles";
        }

        Optional<Vehicle> vehicleOpt = vehicleService.findById(id);
        if (vehicleOpt.isPresent()) {
            Vehicle v = vehicleOpt.get();
            v.setChassisNumber(chassis);
            v.setLicensePlate(plate);
            v.setVehicleModel(vehicleModel);
            v.setPurchasePrice(purchasePrice != null ? purchasePrice : BigDecimal.ZERO);
            v.setRepairCost(repairCost != null ? repairCost : BigDecimal.ZERO);
            v.setSalePrice(salePrice);
            if (status != null)
                v.setStatus(status);

            // Only replace image if a new one is selected
            if (imageFile != null && !imageFile.isEmpty()) {
                String imagePath = saveImage(imageFile);
                v.setImagePath(imagePath);
            }

            vehicleService.save(v);
            ra.addFlashAttribute("successMsg", "Vehicle updated successfully.");
        }
        return "redirect:/inventory/vehicles";
    }

    // Delete vehicle - GET
    @GetMapping("/vehicles/delete/{id}")
    public String deleteVehicle(@PathVariable("id") Long id) {
        vehicleService.deleteById(id);
        return "redirect:/inventory/vehicles";
    }

    // Helper: save uploaded image file, return path
    private String saveImage(MultipartFile imageFile) throws IOException {
        Path uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadRoot);
        String ext = "";
        String original = imageFile.getOriginalFilename();
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf("."));
        }
        String filename = UUID.randomUUID() + ext;
        Path dest = uploadRoot.resolve(filename);
        Files.copy(imageFile.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/" + filename;
    }
}
