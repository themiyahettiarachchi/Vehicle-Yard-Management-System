package com.vyms.controller;

import com.vyms.entity.Sale;
import com.vyms.service.SaleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class SaleController {

    private final SaleService saleService;

    @Autowired
    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @GetMapping("/sales")
    public String listSales(Model model) {
        List<Sale> sales = saleService.findAll();
        model.addAttribute("sales", sales);
        return "sale-list";
    }
}
