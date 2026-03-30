package com.vyms.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "Welcome to the Vehicle Yard Management System (VYMS) API! The server is running successfully.";
    }
}
