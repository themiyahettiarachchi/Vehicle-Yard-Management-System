package com.vyms.controller;

import com.vyms.entity.Vehicle;
import com.vyms.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
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

    // Upload directory: inside the project's static/uploads folder
    private static final String UPLOAD_DIR = "src/main/resources/static/uploads/";

    @Autowired
    public InventoryController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
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

    // Add vehicle - POST
    @PostMapping("/vehicles/add")
    public String addVehicle(
            @RequestParam("chassisNumber") String chassisNumber,
            @RequestParam("licensePlate") String licensePlate,
            @RequestParam("vehicleModel") String vehicleModel,
            @RequestParam(name = "purchasePrice", required = false) BigDecimal purchasePrice,
            @RequestParam(name = "imageFile", required = false) MultipartFile imageFile) throws IOException {

        Vehicle v = new Vehicle();
        v.setChassisNumber(chassisNumber);
        v.setLicensePlate(licensePlate);
        v.setVehicleModel(vehicleModel);
        v.setPurchasePrice(purchasePrice != null ? purchasePrice : BigDecimal.ZERO);
        v.setRepairCost(BigDecimal.ZERO);
        v.setStatus("UNSOLD");

        if (imageFile != null && !imageFile.isEmpty()) {
            String imagePath = saveImage(imageFile);
            v.setImagePath(imagePath);
        }

        vehicleService.save(v);
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
            @RequestParam(name = "imageFile", required = false) MultipartFile imageFile) throws IOException {

        Optional<Vehicle> vehicleOpt = vehicleService.findById(id);
        if (vehicleOpt.isPresent()) {
            Vehicle v = vehicleOpt.get();
            v.setChassisNumber(chassisNumber);
            v.setLicensePlate(licensePlate);
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
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
        String ext = "";
        String original = imageFile.getOriginalFilename();
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf("."));
        }
        String filename = UUID.randomUUID() + ext;
        Path dest = Paths.get(UPLOAD_DIR + filename);
        Files.copy(imageFile.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/" + filename;
    }
}
