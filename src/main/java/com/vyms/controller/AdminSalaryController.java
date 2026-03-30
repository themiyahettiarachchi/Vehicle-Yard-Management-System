package com.vyms.controller;

import com.vyms.entity.*;
import com.vyms.repository.PayrollRepository;
import com.vyms.service.PdfReportService;
import com.vyms.service.RepairService;
import com.vyms.service.SalaryCalculationService;
import com.vyms.service.SaleService;
import com.vyms.service.UserService;
import com.vyms.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Admin-only salary management.
 * Admin views attendance-based payroll and can set worker rates.
 * Managers can ONLY do attendance; contract type is set here by Admin.
 */
@Controller
@RequestMapping("/admin/salary")
public class AdminSalaryController {

    private final UserService userService;
    private final PayrollRepository payrollRepository;
    private final SalaryCalculationService salaryService;
    private final PdfReportService pdfReportService;
    private final SaleService saleService;
    private final VehicleService vehicleService;
    private final RepairService repairService;

    @Autowired
    public AdminSalaryController(UserService userService,
            PayrollRepository payrollRepository,
            SalaryCalculationService salaryService,
            PdfReportService pdfReportService,
            SaleService saleService,
            VehicleService vehicleService,
            RepairService repairService) {
        this.userService = userService;
        this.payrollRepository = payrollRepository;
        this.salaryService = salaryService;
        this.pdfReportService = pdfReportService;
        this.saleService = saleService;
        this.vehicleService = vehicleService;
        this.repairService = repairService;
    }

    // =========================================================================
    // View Payroll
    // =========================================================================
    @GetMapping
    public String salaryPage(@RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "year", required = false) Integer year,
            Model model) {

        int selectedMonth = (month != null) ? month : LocalDate.now().getMonthValue();
        int selectedYear = (year != null) ? year : LocalDate.now().getYear();

        List<Payroll> payrolls = payrollRepository.findByMonthAndYear(selectedMonth, selectedYear);

        BigDecimal totalNet = payrolls.stream()
                .map(p -> p.getNetSalary() != null ? p.getNetSalary() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCost = payrolls.stream()
                .map(Payroll::getEmployerCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // All non-admin staff for rate management
        List<User> staff = userService.findAll().stream()
                .filter(u -> u.getRole() != null && u.getRole() != Role.ADMIN)
                .collect(Collectors.toList());

        model.addAttribute("payrolls", payrolls);
        model.addAttribute("totalNet", totalNet);
        model.addAttribute("totalCost", totalCost);
        model.addAttribute("selectedMonth", selectedMonth);
        model.addAttribute("selectedYear", selectedYear);
        model.addAttribute("staff", staff);
        return "admin/salary";
    }

    // =========================================================================
    // Generate Payroll
    // =========================================================================
    @PostMapping("/generate")
    public String generate(@RequestParam("month") int month,
            @RequestParam("year") int year,
            RedirectAttributes ra) {
        try {
            List<Payroll> result = salaryService.generateMonthlyPayroll(month, year);
            ra.addFlashAttribute("successMsg",
                    "Payroll generated for " + result.size() + " staff member(s).");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Generation failed: " + e.getMessage());
        }
        return "redirect:/admin/salary?month=" + month + "&year=" + year;
    }

    // =========================================================================
    // Update Worker Rates (Admin sets these — Manager cannot change them)
    // =========================================================================
    @PostMapping("/rates/{id}")
    public String updateRates(
            @PathVariable("id") Long id,
            @RequestParam("contractType") String contractType,
            @RequestParam(name = "salaryRate", required = false) BigDecimal salaryRate,
            @RequestParam(name = "dailyWage", required = false) BigDecimal dailyWage,
            @RequestParam(name = "otHourlyRate", required = false) BigDecimal otHourlyRate,
            @RequestParam(name = "month", required = false, defaultValue = "0") int month,
            @RequestParam(name = "year", required = false, defaultValue = "0") int year,
            RedirectAttributes ra) {

        Optional<User> opt = userService.findById(id);
        opt.ifPresent(u -> {
            u.setContractType(contractType);
            if (salaryRate != null)
                u.setSalaryRate(salaryRate);
            if (dailyWage != null)
                u.setDailyWage(dailyWage);
            if (otHourlyRate != null)
                u.setOtHourlyRate(otHourlyRate);
            userService.save(u);
        });
        ra.addFlashAttribute("successMsg", "Rates updated.");
        String redirect = (month > 0 && year > 0)
                ? "redirect:/admin/salary?month=" + month + "&year=" + year
                : "redirect:/admin/salary";
        return redirect;
    }

    // =========================================================================
    // Download: Salary Report PDF
    // =========================================================================
    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadSalaryReport(
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "year",  required = false) Integer year) {

        int m = (month != null) ? month : LocalDate.now().getMonthValue();
        int y = (year  != null) ? year  : LocalDate.now().getYear();

        List<Payroll> payrolls = payrollRepository.findByMonthAndYear(m, y);
        byte[] pdf = pdfReportService.salaryReport(payrolls, m, y);

        String filename = "salary-report-" + y + "-" + String.format("%02d", m) + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // =========================================================================
    // Download: Full Business Report PDF
    // =========================================================================
    @GetMapping("/full-report/download")
    public ResponseEntity<byte[]> downloadFullReport() {
        List<com.vyms.entity.Sale>    sales    = saleService.findAll();
        List<com.vyms.entity.Vehicle> vehicles = vehicleService.findAll();
        List<com.vyms.entity.Repair>  repairs  = repairService.findAll();
        List<Payroll> payrolls = payrollRepository.findAll();

        byte[] pdf = pdfReportService.fullBusinessReport(sales, vehicles, repairs, payrolls);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"full-business-report.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
