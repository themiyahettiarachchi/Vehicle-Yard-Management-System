package com.vms.controller;

import com.vms.service.SalesService;
import com.vms.service.SalesPdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/sales")
public class SalesController {

    private final SalesService salesService;
    private final SalesPdfService salesPdfService;

    public SalesController(SalesService salesService, SalesPdfService salesPdfService) {
        this.salesService = salesService;
        this.salesPdfService = salesPdfService;
    }

    @GetMapping
    public String salesPage(Model model) {
        model.addAttribute("totalSales", salesService.getTotalSalesCount());
        model.addAttribute("totalRevenue", salesService.getTotalRevenue());
        model.addAttribute("availableVehicles", salesService.getAvailableVehicleCount());
        model.addAttribute("salesRecords", salesService.getAllSales());
        model.addAttribute("availableVehicleList", salesService.getAvailableVehicles());

        model.addAttribute("activePage", "sales");
        model.addAttribute("userName", "Sales Lead");
        model.addAttribute("userRole", "Sales");

        return "sales";
    }

    @PostMapping("/record")
    public String recordSale(@RequestParam("vehicleId") Integer vehicleId,
            @RequestParam("buyerType") String buyerType,
            @RequestParam("salePrice") BigDecimal salePrice,
            @RequestParam("customerName") String customerName,
            @RequestParam("contactNumber") String contactNumber,
            @RequestParam(value = "email", required = false) String email,
            RedirectAttributes redirectAttributes) {
        try {
            salesService.recordSale(vehicleId, buyerType, salePrice, customerName, contactNumber);
            redirectAttributes.addFlashAttribute("successMessage", "Sale recorded successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to record sale: " + e.getMessage());
        }
        return "redirect:/sales";
    }

    @GetMapping("/report/pdf")
    public ResponseEntity<byte[]> downloadSalesReport() {
        byte[] pdfBytes = salesPdfService.generateSalesReport(
                salesService.getAllSales(),
                salesService.getTotalSalesCount(),
                salesService.getTotalRevenue(),
                salesService.getAvailableVehicleCount());

        String filename = "Sales_Report_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.length)
                .body(pdfBytes);
    }
}
