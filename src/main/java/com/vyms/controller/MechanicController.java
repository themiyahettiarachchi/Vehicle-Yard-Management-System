package com.vyms.controller;

import com.vyms.entity.Repair;
import com.vyms.entity.Vehicle;
import com.vyms.service.PdfReportService;
import com.vyms.service.RepairService;
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

@Controller
@RequestMapping("/mechanic")
public class MechanicController {

    private final RepairService repairService;
    private final VehicleService vehicleService;
    private final PdfReportService pdfReportService;

    @Autowired
    public MechanicController(RepairService repairService, VehicleService vehicleService,
                              PdfReportService pdfReportService) {
        this.repairService = repairService;
        this.vehicleService = vehicleService;
        this.pdfReportService = pdfReportService;
    }

    // Dashboard
    @GetMapping
    public String dashboard(Model model) {
        // Fetch vehicles for the dashboard to track inspection status
        List<Vehicle> vehicles = vehicleService.findAll();
        
        long uninspected = vehicles.stream().filter(v -> v.getRepairs() == null || v.getRepairs().isEmpty()).count();
        long pending = vehicles.stream().filter(v -> v.getRepairs() != null && v.getRepairs().stream().anyMatch(r -> "PENDING".equalsIgnoreCase(r.getStatus()))).count();
        long fullyInspected = vehicles.stream().filter(v -> v.getRepairs() != null && !v.getRepairs().isEmpty() && v.getRepairs().stream().noneMatch(r -> "PENDING".equalsIgnoreCase(r.getStatus()))).count();

        model.addAttribute("vehicles", vehicles);
        model.addAttribute("totalVehicles", vehicles.size());
        model.addAttribute("uninspected", uninspected);
        model.addAttribute("pendingRepairs", pending);
        model.addAttribute("fullyInspected", fullyInspected);
        return "mechanic";
    }

    // Repair & Maintenance page
    @GetMapping("/repairs")
    public String repairs(Model model, @RequestParam(name = "vehicleId", required = false) Long vehicleId) {
        List<Repair> repairs = repairService.findAll();
        List<Vehicle> vehicles = vehicleService.findAll();

        // Totals
        BigDecimal totalExpenses = repairs.stream()
                .map(r -> r.getCost() != null ? r.getCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal internalTotal = repairs.stream()
                .filter(r -> "INTERNAL".equalsIgnoreCase(r.getRepairType()))
                .map(r -> r.getCost() != null ? r.getCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal externalTotal = repairs.stream()
                .filter(r -> "EXTERNAL".equalsIgnoreCase(r.getRepairType()))
                .map(r -> r.getCost() != null ? r.getCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("repairs", repairs);
        model.addAttribute("vehicles", vehicles);
        model.addAttribute("totalExpenses", totalExpenses);
        model.addAttribute("internalTotal", internalTotal);
        model.addAttribute("externalTotal", externalTotal);
        model.addAttribute("selectedVehicleId", vehicleId);

        return "mechanic/repairs";
    }

    // Log a new repair expense
    @PostMapping("/repairs/log")
    public String logRepair(
            @RequestParam("vehicleId") Long vehicleId,
            @RequestParam("description") String description,
            @RequestParam("cost") BigDecimal cost,
            @RequestParam("repairType") String repairType) {

        Optional<Vehicle> vehicleOpt = vehicleService.findById(vehicleId);
        if (vehicleOpt.isPresent()) {
            Repair repair = new Repair();
            repair.setVehicle(vehicleOpt.get());
            repair.setDescription(description);
            repair.setCost(cost);
            repair.setRepairType(repairType.toUpperCase());
            repair.setStatus("PENDING");
            repair.setRepairDate(LocalDate.now());
            repairService.save(repair);

            // Update vehicle's repair cost total
            Vehicle v = vehicleOpt.get();
            BigDecimal current = v.getRepairCost() != null ? v.getRepairCost() : BigDecimal.ZERO;
            v.setRepairCost(current.add(cost));
            vehicleService.save(v);
        }
        return "redirect:/mechanic/repairs";
    }

    // Mark repair as Inspected
    @GetMapping("/repairs/inspect/{id}")
    public String markInspected(@PathVariable("id") Long id) {
        Optional<Repair> repOpt = repairService.findById(id);
        repOpt.ifPresent(r -> {
            r.setStatus("INSPECTED");
            repairService.save(r);
        });
        return "redirect:/mechanic/repairs";
    }

    // Edit a repair entry
    @PostMapping("/repairs/edit/{id}")
    public String editRepair(@PathVariable("id") Long id,
            @RequestParam("description") String description,
            @RequestParam("cost") BigDecimal cost,
            @RequestParam("repairType") String repairType,
            @RequestParam(name = "status", required = false) String status) {
        Optional<Repair> repOpt = repairService.findById(id);
        repOpt.ifPresent(r -> {
            r.setDescription(description);
            r.setCost(cost);
            r.setRepairType(repairType);
            if (status != null) {
                r.setStatus(status);
            }
            repairService.save(r);
        });
        return "redirect:/mechanic/repairs";
    }

    // =========================================================================
    // Download: Repair Report PDF
    // =========================================================================
    @GetMapping("/repairs/download")
    public ResponseEntity<byte[]> downloadRepairReport() {
        List<Repair> repairs = repairService.findAll();
        byte[] pdf = pdfReportService.repairReport(repairs);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"repair-report.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
