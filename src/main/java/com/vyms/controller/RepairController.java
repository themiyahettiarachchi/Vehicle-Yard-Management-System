package com.vyms.controller;

import com.vyms.entity.Repair;
import com.vyms.service.RepairService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class RepairController {

    private final RepairService repairService;

    @Autowired
    public RepairController(RepairService repairService) {
        this.repairService = repairService;
    }

    @GetMapping("/repairs")
    public String listRepairs(Model model) {
        List<Repair> repairs = repairService.findAll();
        model.addAttribute("repairs", repairs);
        return "repair-list";
    }
}
