package com.vyms.controller;

import com.vyms.entity.Role;
import com.vyms.entity.User;
import com.vyms.service.SystemLogService;
import com.vyms.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserService userService;
    private final SystemLogService logService;

    // Validation
    private static final Pattern EMP_EMAIL_PATTERN = Pattern
            .compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern EMP_NAME_PATTERN = Pattern.compile("^[A-Za-z .'-]{2,100}$");

    private boolean isValidEmployeeInput(String name, String email, String password, boolean passwordRequired) {
        if (name == null || !EMP_NAME_PATTERN.matcher(name.trim()).matches())
            return false;
        if (email == null || !EMP_EMAIL_PATTERN.matcher(email.trim()).matches())
            return false;
        if (passwordRequired && (password == null || password.length() < 8))
            return false;
        if (!passwordRequired && password != null && !password.isEmpty() && password.length() < 8)
            return false;
        return true;
    }

    @Autowired
    public AdminUserController(UserService userService, SystemLogService logService) {
        this.userService = userService;
        this.logService = logService;
    }

    @GetMapping
    public String listUsers(Model model) {
        var users = userService.findAll();
        model.addAttribute("users", users);
        model.addAttribute("roles", Role.values());

        // Role distribution counts
        model.addAttribute("countAdmin", users.stream().filter(u -> u.getRole() == Role.ADMIN).count());
        model.addAttribute("countManager", users.stream().filter(u -> u.getRole() == Role.MANAGER).count());
        model.addAttribute("countSales", users.stream().filter(u -> u.getRole() == Role.SALES).count());
        model.addAttribute("countInventory", users.stream().filter(u -> u.getRole() == Role.INVENTORY).count());
        model.addAttribute("countMechanic", users.stream().filter(u -> u.getRole() == Role.MECHANIC).count());
        return "admin/users";
    }

    @PostMapping("/add")
    public String addUser(@RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("role") Role role,
            @RequestParam(name = "contractType", required = false) String contractType,
            @RequestParam(name = "salaryRate", required = false) BigDecimal salaryRate) {
        if (!isValidEmployeeInput(username, email, password, true)) {
            return "redirect:/admin/users";
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        user.setRole(role);
        user.setContractType(contractType != null ? contractType : "PERMANENT");
        user.setSalaryRate(salaryRate);

        userService.save(user);
        logService.createLog("USER_CREATED", "Admin created user: " + email, "Admin", "SUCCESS");
        return "redirect:/admin/users";
    }

    @GetMapping("/delete/{id}")
    public String deleteUser(@PathVariable("id") Long id) {
        Optional<User> userOpt = userService.findById(id);
        if (userOpt.isPresent()) {
            String email = userOpt.get().getEmail();
            userService.deleteById(id);
            logService.createLog("USER_DELETED", "Admin deleted user: " + email, "Admin", "SUCCESS");
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/edit/{id}")
    public String editUserForm(@PathVariable("id") Long id, Model model) {
        Optional<User> userOpt = userService.findById(id);
        if (userOpt.isPresent()) {
            model.addAttribute("user", userOpt.get());
            model.addAttribute("roles", Role.values());
            return "admin/users-edit";
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/edit/{id}")
    public String updateUser(@PathVariable("id") Long id,
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("role") Role role,
            @RequestParam(name = "contractType", required = false) String contractType,
            @RequestParam(name = "salaryRate", required = false) BigDecimal salaryRate,
            @RequestParam(name = "password", required = false) String password) {
        if (!isValidEmployeeInput(username, email, password, false)) {
            return "redirect:/admin/users";
        }
        Optional<User> userOpt = userService.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setUsername(username);
            user.setEmail(email);
            user.setRole(role);
            user.setContractType(contractType != null ? contractType : "PERMANENT");
            user.setSalaryRate(salaryRate);

            if (password != null && !password.trim().isEmpty()) {
                user.setPassword(password);
            }

            userService.save(user);
            logService.createLog("USER_UPDATED", "Admin updated user: " + email, "Admin", "SUCCESS");
        }
        return "redirect:/admin/users";
    }
}
