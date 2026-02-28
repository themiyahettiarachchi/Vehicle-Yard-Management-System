package com.vms.controller;

import com.vms.service.SalesService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final SalesService salesService;

    public DashboardController(SalesService salesService) {
        this.salesService = salesService;
    }

    @GetMapping({ "/", "/dashboard" })
    public String dashboard(Model model) {
        model.addAttribute("totalVehicles", salesService.getTotalVehicleCount());
        model.addAttribute("totalSales", salesService.getTotalSalesCount());
        model.addAttribute("totalRevenue", salesService.getTotalRevenue());
        model.addAttribute("totalProfit", salesService.getTotalProfit());

        // Chart data
        model.addAttribute("monthLabels", salesService.getMonthLabels());
        model.addAttribute("monthlySalesData", salesService.getMonthlySalesData());
        model.addAttribute("monthlyRevenueData", salesService.getMonthlyRevenueData());

        // Activity feed
        model.addAttribute("activityFeed", salesService.getActivityFeed());

        model.addAttribute("activePage", "dashboard");
        model.addAttribute("userName", "Sales Lead");
        model.addAttribute("userRole", "Sales");

        return "dashboard";
    }
}
