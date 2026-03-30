package com.vyms.controller;

import com.vyms.entity.Payroll;
import com.vyms.service.PayrollService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class PayrollController {

    private final PayrollService payrollService;

    @Autowired
    public PayrollController(PayrollService payrollService) {
        this.payrollService = payrollService;
    }

    @GetMapping("/payrolls")
    public String listPayrolls(Model model) {
        List<Payroll> payrolls = payrollService.findAll();
        model.addAttribute("payrolls", payrolls);
        return "payroll-list";
    }
}
