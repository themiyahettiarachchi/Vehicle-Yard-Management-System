package com.vyms.controller;

import com.vyms.service.SystemLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/logs")
public class SystemLogController {

    private final SystemLogService logService;

    @Autowired
    public SystemLogController(SystemLogService logService) {
        this.logService = logService;
    }

    @GetMapping
    public String listLogs(Model model) {
        model.addAttribute("logs", logService.findAllLogs());
        return "admin/logs";
    }
}
