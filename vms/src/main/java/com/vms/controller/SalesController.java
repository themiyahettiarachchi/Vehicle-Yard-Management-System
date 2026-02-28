package com.vms.controller;

import com.vms.service.SalesService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/sales")
public class SalesController {

    private final SalesService salesService;

    public SalesController(SalesService salesService) {
        this.salesService = salesService;
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
}
