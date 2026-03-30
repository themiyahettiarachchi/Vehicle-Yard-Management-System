package com.vyms.controller;

import com.vyms.entity.Attendance;
import com.vyms.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class AttendanceController {

    private final AttendanceService attendanceService;

    @Autowired
    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @GetMapping("/attendances")
    public String listAttendances(Model model) {
        List<Attendance> attendances = attendanceService.findAll();
        model.addAttribute("attendances", attendances);
        return "attendance-list";
    }
}
