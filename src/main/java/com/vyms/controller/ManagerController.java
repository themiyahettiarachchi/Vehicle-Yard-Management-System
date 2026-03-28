package com.vyms.controller;

import com.vyms.entity.*;
import com.vyms.repository.AttendanceRepository;
import com.vyms.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/manager")
public class ManagerController {

    private final UserService userService;
    private final AttendanceService attendanceService;
    private final AttendanceRepository attendanceRepository;
    private final VehicleService vehicleService;
    private final SaleService saleService;
    private final RepairService repairService;
    private final PdfReportService pdfReportService;

    @Autowired
    public ManagerController(UserService userService,
            AttendanceService attendanceService,
            AttendanceRepository attendanceRepository,
            VehicleService vehicleService,
            SaleService saleService,
            RepairService repairService,
            PdfReportService pdfReportService) {
        this.userService = userService;
        this.attendanceService = attendanceService;
        this.attendanceRepository = attendanceRepository;
        this.vehicleService = vehicleService;
        this.saleService = saleService;
        this.repairService = repairService;
        this.pdfReportService = pdfReportService;
    }

    // =========================================================================
    // Dashboard
    // =========================================================================
    @GetMapping
    public String dashboard(Model model) {
        List<Vehicle> vehicles = vehicleService.findAll();
        List<Sale> sales = saleService.findAll();

        BigDecimal totalRevenue = sales.stream()
                .map(s -> s.getSalePrice() != null ? s.getSalePrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalProfit = sales.stream()
                .map(Sale::getProfit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("totalVehicles", vehicles.size());
        model.addAttribute("totalSales", sales.size());
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalProfit", totalProfit);
        model.addAttribute("recentSales", sales.stream()
                .sorted(Comparator.comparing(s -> s.getSaleDate() == null ? LocalDate.MIN : s.getSaleDate(),
                        Comparator.reverseOrder()))
                .limit(5).collect(Collectors.toList()));
        return "manager/dashboard";
    }

    // =========================================================================
    // Attendance – GET
    // =========================================================================
    @GetMapping("/attendance")
    public String attendance(@RequestParam(name = "error", required = false) String error,
            Model model) {
        List<User> staff = userService.findAll().stream()
                .filter(u -> u.getRole() != Role.ADMIN)
                .collect(Collectors.toList());

        LocalDate today = LocalDate.now();
        List<Attendance> todayRecords = attendanceRepository.findByDate(today);

        long present = todayRecords.stream().filter(a -> "PRESENT".equalsIgnoreCase(a.getStatus())).count();
        long absent = todayRecords.stream().filter(a -> "ABSENT".equalsIgnoreCase(a.getStatus())).count();
        long offDay = todayRecords.stream().filter(a -> "OFF_DAY".equalsIgnoreCase(a.getStatus())).count();
        long halfDay = todayRecords.stream().filter(a -> "HALF_DAY".equalsIgnoreCase(a.getStatus())).count();

        Map<Long, String> todayStatusMap = new HashMap<>();
        Map<Long, Attendance> todayRecordMap = new HashMap<>();
        for (Attendance att : todayRecords) {
            if (att.getUser() != null) {
                todayStatusMap.put(att.getUser().getId(), att.getStatus());
                todayRecordMap.put(att.getUser().getId(), att);
            }
        }

        List<String> weekDates = new ArrayList<>();
        List<Long> weekPresent = new ArrayList<>();
        List<Long> weekAbsent = new ArrayList<>();
        List<Long> weekOffDay = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            List<Attendance> recs = attendanceRepository.findByDate(d);
            weekDates.add(d.getMonthValue() + "/" + d.getDayOfMonth());
            weekPresent.add(recs.stream().filter(a -> "PRESENT".equalsIgnoreCase(a.getStatus())).count());
            weekAbsent.add(recs.stream().filter(a -> "ABSENT".equalsIgnoreCase(a.getStatus())).count());
            weekOffDay.add(recs.stream().filter(a -> "OFF_DAY".equalsIgnoreCase(a.getStatus())).count());
        }

        model.addAttribute("staff", staff);
        model.addAttribute("totalStaff", staff.size());
        model.addAttribute("presentCount", present);
        model.addAttribute("absentCount", absent);
        model.addAttribute("offDayCount", offDay);
        model.addAttribute("halfDayCount", halfDay);
        model.addAttribute("todayRecords", todayRecords);
        model.addAttribute("todayStatusMap", todayStatusMap);
        model.addAttribute("todayRecordMap", todayRecordMap);
        model.addAttribute("weekDates", weekDates);
        model.addAttribute("weekPresent", weekPresent);
        model.addAttribute("weekAbsent", weekAbsent);
        model.addAttribute("weekOffDay", weekOffDay);
        model.addAttribute("today", today.toString());
        model.addAttribute("activeTab", "attendance");
        model.addAttribute("errorMsg", error);
        return "manager/attendance-salary";
    }

    // =========================================================================
    // Attendance – State Machine Endpoints
    // =========================================================================
    @PostMapping("/attendance/checkin/{userId}")
    public String checkIn(@PathVariable("userId") Long userId, RedirectAttributes ra) {
        try {
            attendanceService.checkIn(userId);
        } catch (IllegalStateException e) {
            ra.addAttribute("error", e.getMessage());
        }
        return "redirect:/manager/attendance";
    }

    @PostMapping("/attendance/checkout/{userId}")
    public String checkOut(@PathVariable("userId") Long userId, RedirectAttributes ra) {
        try {
            attendanceService.checkOut(userId);
        } catch (IllegalStateException e) {
            ra.addAttribute("error", e.getMessage());
        }
        return "redirect:/manager/attendance";
    }

    @PostMapping("/attendance/offday/{userId}")
    public String markOffDay(@PathVariable("userId") Long userId, RedirectAttributes ra) {
        try {
            attendanceService.markOffDay(userId);
        } catch (IllegalStateException e) {
            ra.addAttribute("error", e.getMessage());
        }
        return "redirect:/manager/attendance";
    }

    @PostMapping("/attendance/clear/{userId}")
    public String clearAttendance(@PathVariable("userId") Long userId, RedirectAttributes ra) {
        try {
            attendanceService.clearTodayAttendance(userId);
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addAttribute("error", e.getMessage());
        }
        return "redirect:/manager/attendance";
    }

    // =========================================================================
    // Attendance – History / Search
    // =========================================================================
    @GetMapping("/attendance/history")
    public String attendanceHistory(
            @RequestParam(name = "month", required = false) String month,
            @RequestParam(name = "fromDate", required = false) String fromDateStr,
            @RequestParam(name = "toDate", required = false) String toDateStr,
            Model model) {

        LocalDate from;
        LocalDate to;

        if (month != null && !month.isBlank()) {
            // "month" param is YYYY-MM
            java.time.YearMonth ym = java.time.YearMonth.parse(month);
            from = ym.atDay(1);
            to = ym.atEndOfMonth();
        } else if (fromDateStr != null && !fromDateStr.isBlank() && toDateStr != null && !toDateStr.isBlank()) {
            from = LocalDate.parse(fromDateStr);
            to = LocalDate.parse(toDateStr);
        } else {
            // default: last 30 days
            to = LocalDate.now();
            from = to.minusDays(29);
        }

        // Ensure from <= to
        if (from.isAfter(to)) {
            LocalDate tmp = from; from = to; to = tmp;
        }

        List<Attendance> historyRecords = attendanceRepository.findByDateBetween(from, to);
        // Sort newest first
        historyRecords.sort(Comparator.comparing(Attendance::getDate).reversed());

        // Re-load the standard attendance page model so sidebar/chart/today table still show
        List<User> staff = userService.findAll().stream()
                .filter(u -> u.getRole() != Role.ADMIN)
                .collect(Collectors.toList());

        LocalDate today = LocalDate.now();
        List<Attendance> todayRecords = attendanceRepository.findByDate(today);
        long present = todayRecords.stream().filter(a -> "PRESENT".equalsIgnoreCase(a.getStatus())).count();
        long absent  = todayRecords.stream().filter(a -> "ABSENT".equalsIgnoreCase(a.getStatus())).count();
        long offDay  = todayRecords.stream().filter(a -> "OFF_DAY".equalsIgnoreCase(a.getStatus())).count();
        long halfDay = todayRecords.stream().filter(a -> "HALF_DAY".equalsIgnoreCase(a.getStatus())).count();

        Map<Long, String> todayStatusMap = new HashMap<>();
        Map<Long, Attendance> todayRecordMap = new HashMap<>();
        for (Attendance att : todayRecords) {
            if (att.getUser() != null) {
                todayStatusMap.put(att.getUser().getId(), att.getStatus());
                todayRecordMap.put(att.getUser().getId(), att);
            }
        }

        List<String> weekDates = new ArrayList<>();
        List<Long> weekPresent = new ArrayList<>(), weekAbsent = new ArrayList<>(), weekOffDay = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            List<Attendance> recs = attendanceRepository.findByDate(d);
            weekDates.add(d.getMonthValue() + "/" + d.getDayOfMonth());
            weekPresent.add(recs.stream().filter(a -> "PRESENT".equalsIgnoreCase(a.getStatus())).count());
            weekAbsent.add(recs.stream().filter(a -> "ABSENT".equalsIgnoreCase(a.getStatus())).count());
            weekOffDay.add(recs.stream().filter(a -> "OFF_DAY".equalsIgnoreCase(a.getStatus())).count());
        }

        model.addAttribute("staff", staff);
        model.addAttribute("totalStaff", staff.size());
        model.addAttribute("presentCount", present);
        model.addAttribute("absentCount", absent);
        model.addAttribute("offDayCount", offDay);
        model.addAttribute("halfDayCount", halfDay);
        model.addAttribute("todayRecords", todayRecords);
        model.addAttribute("todayStatusMap", todayStatusMap);
        model.addAttribute("todayRecordMap", todayRecordMap);
        model.addAttribute("weekDates", weekDates);
        model.addAttribute("weekPresent", weekPresent);
        model.addAttribute("weekAbsent", weekAbsent);
        model.addAttribute("weekOffDay", weekOffDay);
        model.addAttribute("today", today.toString());
        model.addAttribute("activeTab", "attendance");
        model.addAttribute("historyRecords", historyRecords);
        model.addAttribute("historyFrom", from.toString());
        model.addAttribute("historyTo", to.toString());
        model.addAttribute("historyMonth", month != null ? month : "");
        return "manager/attendance-salary";
    }

    // =========================================================================
    // Inventory
    // =========================================================================
    @GetMapping("/inventory")
    public String inventory(Model model) {
        model.addAttribute("vehicles", vehicleService.findAll());
        return "manager/inventory-reports";
    }

    @PostMapping("/inventory/edit/{id}")
    public String editVehicle(@PathVariable("id") Long id,
            @RequestParam("vehicleModel") String vehicleModel,
            @RequestParam("licensePlate") String licensePlate,
            @RequestParam(name = "purchasePrice", required = false) BigDecimal purchasePrice,
            @RequestParam(name = "repairCost", required = false) BigDecimal repairCost,
            @RequestParam(name = "status", required = false) String status) {
        Optional<Vehicle> vOpt = vehicleService.findById(id);
        vOpt.ifPresent(v -> {
            v.setVehicleModel(vehicleModel);
            v.setLicensePlate(licensePlate);
            if (purchasePrice != null)
                v.setPurchasePrice(purchasePrice);
            if (repairCost != null)
                v.setRepairCost(repairCost);
            if (status != null)
                v.setStatus(status);
            vehicleService.save(v);
        });
        return "redirect:/manager/inventory";
    }

    // =========================================================================
    // Repair & Maintenance
    // =========================================================================
    @GetMapping("/repair")
    public String repair(Model model) {
        List<Repair> repairs = repairService.findAll();
        List<Vehicle> vehicles = vehicleService.findAll();

        long totalRepairs = repairs.size();
        long pendingRepairs = repairs.stream().filter(r -> "PENDING".equalsIgnoreCase(r.getStatus())).count();
        long inspectedRepairs = repairs.stream().filter(r -> "INSPECTED".equalsIgnoreCase(r.getStatus())).count();
        BigDecimal totalRepairCost = repairs.stream()
                .map(r -> r.getCost() != null ? r.getCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("repairs", repairs);
        model.addAttribute("vehicles", vehicles);
        model.addAttribute("totalRepairs", totalRepairs);
        model.addAttribute("pendingRepairs", pendingRepairs);
        model.addAttribute("inspectedRepairs", inspectedRepairs);
        model.addAttribute("totalRepairCost", totalRepairCost);
        return "manager/repair";
    }

    @PostMapping("/repair/edit/{id}")
    public String editRepairFromRepairPage(@PathVariable("id") Long id,
            @RequestParam("description") String description,
            @RequestParam("cost") BigDecimal cost,
            @RequestParam("repairType") String repairType,
            @RequestParam("status") String status) {
        Optional<Repair> repOpt = repairService.findById(id);
        repOpt.ifPresent(r -> {
            r.setDescription(description);
            r.setCost(cost);
            r.setRepairType(repairType);
            r.setStatus(status);
            repairService.save(r);
        });
        return "redirect:/manager/repair";
    }

    // =========================================================================
    // Reports
    // =========================================================================
    @GetMapping("/reports")
    public String reports(Model model) {
        List<Sale> sales = saleService.findAll();
        List<Vehicle> vehicles = vehicleService.findAll();
        List<Repair> repairs = repairService.findAll();

        BigDecimal totalRevenue = sales.stream()
                .map(s -> s.getSalePrice() != null ? s.getSalePrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpenses = sales.stream()
                .filter(s -> s.getVehicle() != null || s.getTotalCost() != null)
                .map(s -> {
                    if (s.getTotalCost() != null)
                        return s.getTotalCost();
                    Vehicle v = s.getVehicle();
                    return (v.getPurchasePrice() != null ? v.getPurchasePrice() : BigDecimal.ZERO)
                            .add(v.getRepairCost() != null ? v.getRepairCost() : BigDecimal.ZERO);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalProfit = sales.stream().map(Sale::getProfit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgProfitPerSale = sales.isEmpty() ? BigDecimal.ZERO
                : totalProfit.divide(BigDecimal.valueOf(sales.size()), 2, java.math.RoundingMode.HALF_UP);

        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalExpenses", totalExpenses);
        model.addAttribute("totalProfit", totalProfit);
        model.addAttribute("avgProfitPerSale", avgProfitPerSale);
        model.addAttribute("totalSales", sales.size());

        long totalVehicles = vehicles.size();
        long soldVehicles = vehicles.stream().filter(v -> "SOLD".equalsIgnoreCase(v.getStatus())).count();
        BigDecimal totalRepairCost = repairs.stream()
                .map(r -> r.getCost() != null ? r.getCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("totalVehicles", totalVehicles);
        model.addAttribute("soldVehicles", soldVehicles);
        model.addAttribute("unsoldVehicles", totalVehicles - soldVehicles);
        model.addAttribute("totalRepairCost", totalRepairCost);
        model.addAttribute("totalRepairs", repairs.size());

        Map<String, Long> buyerTypeCounts = new LinkedHashMap<>();
        for (BuyerType bt : BuyerType.values())
            buyerTypeCounts.put(bt.name(), 0L);
        sales.stream().filter(s -> s.getBuyerType() != null)
                .forEach(s -> buyerTypeCounts.merge(s.getBuyerType().name(), 1L, Long::sum));
        model.addAttribute("buyerTypeLabels", new ArrayList<>(buyerTypeCounts.keySet()));
        model.addAttribute("buyerTypeValues", new ArrayList<>(buyerTypeCounts.values()));
        model.addAttribute("buyerTypes", BuyerType.values());

        LocalDate now = LocalDate.now();
        java.time.format.DateTimeFormatter labelFmt = java.time.format.DateTimeFormatter.ofPattern("MMM yyyy");
        List<String> monthLabels = new ArrayList<>();
        List<BigDecimal> monthlyRevenue = new ArrayList<>(), monthlyProfit = new ArrayList<>(),
                monthlyRepairCost = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            int y = month.getYear(), m = month.getMonthValue();
            monthLabels.add(month.format(labelFmt));
            monthlyRevenue.add(sales.stream()
                    .filter(s -> s.getSaleDate() != null && s.getSaleDate().getYear() == y
                            && s.getSaleDate().getMonthValue() == m)
                    .map(s -> s.getSalePrice() != null ? s.getSalePrice() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            monthlyProfit.add(sales.stream()
                    .filter(s -> s.getSaleDate() != null && s.getSaleDate().getYear() == y
                            && s.getSaleDate().getMonthValue() == m)
                    .map(Sale::getProfit).reduce(BigDecimal.ZERO, BigDecimal::add));
            monthlyRepairCost.add(repairs.stream()
                    .filter(r -> r.getRepairDate() != null && r.getRepairDate().getYear() == y
                            && r.getRepairDate().getMonthValue() == m)
                    .map(r -> r.getCost() != null ? r.getCost() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
        }
        model.addAttribute("monthLabels", monthLabels);
        model.addAttribute("monthlyRevenue", monthlyRevenue);
        model.addAttribute("monthlyProfit", monthlyProfit);
        model.addAttribute("monthlyRepairCost", monthlyRepairCost);

        List<Map<String, Object>> profitRows = sales.stream()
                .filter(s -> s.getVehicle() != null && s.getSalePrice() != null)
                .map(s -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    Vehicle v = s.getVehicle();
                    BigDecimal profit = s.getProfit();
                    BigDecimal margin = s.getSalePrice().compareTo(BigDecimal.ZERO) > 0
                            ? profit.multiply(BigDecimal.valueOf(100)).divide(s.getSalePrice(), 1,
                                    java.math.RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    row.put("vehicle", v.getVehicleModel() != null ? v.getVehicleModel() : "—");
                    row.put("buyerType", s.getBuyerType() != null ? s.getBuyerType().name() : "—");
                    row.put("purchase", v.getPurchasePrice() != null ? v.getPurchasePrice() : BigDecimal.ZERO);
                    row.put("repair", v.getRepairCost() != null ? v.getRepairCost() : BigDecimal.ZERO);
                    row.put("salePrice", s.getSalePrice());
                    row.put("profit", profit);
                    row.put("margin", margin);
                    row.put("saleDate", s.getSaleDate() != null ? s.getSaleDate().toString() : "—");
                    return row;
                }).collect(Collectors.toList());
        model.addAttribute("profitRows", profitRows);

        List<Repair> recentRepairs = repairs.stream()
                .filter(r -> r.getRepairDate() != null)
                .sorted(Comparator.comparing(Repair::getRepairDate).reversed())
                .limit(5).collect(Collectors.toList());
        model.addAttribute("recentRepairs", recentRepairs);
        model.addAttribute("repairs", repairs);
        model.addAttribute("sales", sales);
        return "manager/mechanic-sale-reports";
    }

    @PostMapping("/reports/repair/edit/{id}")
    public String editRepair(@PathVariable("id") Long id,
            @RequestParam("description") String description,
            @RequestParam("cost") BigDecimal cost,
            @RequestParam("repairType") String repairType,
            @RequestParam("status") String status) {
        Optional<Repair> repOpt = repairService.findById(id);
        repOpt.ifPresent(r -> {
            r.setDescription(description);
            r.setCost(cost);
            r.setRepairType(repairType);
            r.setStatus(status);
            repairService.save(r);
        });
        return "redirect:/manager/reports";
    }

    @PostMapping("/reports/sale/edit/{id}")
    public String editSaleFromReports(@PathVariable("id") Long id,
            @RequestParam("salePrice") BigDecimal salePrice,
            @RequestParam("saleStatus") String saleStatus,
            @RequestParam("buyerType") String buyerType,
            @RequestParam("customerName") String customerName) {
        Optional<Sale> saleOpt = saleService.findById(id);
        saleOpt.ifPresent(s -> {
            s.setSalePrice(salePrice);
            s.setSaleStatus(saleStatus);
            s.setBuyerType(BuyerType.valueOf(buyerType));
            s.setCustomerName(customerName);
            saleService.save(s);
        });
        return "redirect:/manager/reports";
    }

    // =========================================================================
    // Sales Management
    // =========================================================================
    @GetMapping("/sales")
    public String salesManagement(Model model) {
        List<Sale> sales = saleService.findAll();
        List<Vehicle> vehicles = vehicleService.findAll();
        List<Vehicle> unsold = vehicles.stream().filter(v -> !"SOLD".equalsIgnoreCase(v.getStatus()))
                .collect(Collectors.toList());
        BigDecimal totalRevenue = sales.stream()
                .map(s -> s.getSalePrice() != null ? s.getSalePrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("sales", sales);
        model.addAttribute("vehicles", unsold);
        model.addAttribute("buyerTypes", BuyerType.values());
        model.addAttribute("totalSales", sales.size());
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("availableVehicles", unsold.size());
        return "manager/sales-management";
    }

    @PostMapping("/sales/record")
    public String recordSale(@RequestParam("vehicleId") Long vehicleId,
            @RequestParam("salePrice") BigDecimal salePrice,
            @RequestParam("buyerType") String buyerType,
            @RequestParam("customerName") String customerName,
            @RequestParam("contactNumber") String contactNumber,
            @RequestParam(name = "email", required = false) String email) {
        Optional<Vehicle> vehicleOpt = vehicleService.findById(vehicleId);
        if (vehicleOpt.isPresent()) {
            Vehicle v = vehicleOpt.get();
            BigDecimal snapshotCost = (v.getPurchasePrice() != null ? v.getPurchasePrice() : BigDecimal.ZERO)
                    .add(v.getRepairCost() != null ? v.getRepairCost() : BigDecimal.ZERO);
            Sale sale = new Sale();
            sale.setVehicle(v);
            sale.setSalePrice(salePrice);
            sale.setBuyerType(BuyerType.valueOf(buyerType));
            sale.setCustomerName(customerName);
            sale.setContactNumber(contactNumber);
            sale.setEmail(email);
            sale.setSaleDate(LocalDate.now());
            sale.setSaleStatus("FINALIZED");
            sale.setTotalCost(snapshotCost);
            v.setStatus("SOLD");
            v.setSalePrice(salePrice);
            vehicleService.save(v);
            saleService.save(sale);
        }
        return "redirect:/manager/sales";
    }

    @PostMapping("/sales/edit/{id}")
    public String editSale(@PathVariable("id") Long id,
            @RequestParam("salePrice") BigDecimal salePrice,
            @RequestParam("buyerType") String buyerType,
            @RequestParam("customerName") String customerName,
            @RequestParam(name = "contactNumber", required = false) String contactNumber,
            @RequestParam(name = "email", required = false) String email,
            @RequestParam(name = "saleStatus", required = false) String saleStatus) {
        Optional<Sale> saleOpt = saleService.findById(id);
        saleOpt.ifPresent(s -> {
            s.setSalePrice(salePrice);
            s.setBuyerType(BuyerType.valueOf(buyerType));
            s.setCustomerName(customerName);
            if (contactNumber != null)
                s.setContactNumber(contactNumber);
            if (email != null)
                s.setEmail(email);
            if (saleStatus != null)
                s.setSaleStatus(saleStatus);
            if (s.getVehicle() != null) {
                s.getVehicle().setSalePrice(salePrice);
                vehicleService.save(s.getVehicle());
            }
            saleService.save(s);
        });
        return "redirect:/manager/sales";
    }

    @GetMapping("/sales/invoice/{id}")
    public String invoice(@PathVariable("id") Long id, Model model) {
        Optional<Sale> saleOpt = saleService.findById(id);
        if (saleOpt.isPresent()) {
            model.addAttribute("sale", saleOpt.get());
            return "sales/invoice";
        }
        return "redirect:/manager/sales";
    }

    @GetMapping("/sales/api/vehicle/{id}")
    @ResponseBody
    public String vehicleData(@PathVariable("id") Long id) {
        Optional<Vehicle> opt = vehicleService.findById(id);
        if (opt.isPresent()) {
            Vehicle v = opt.get();
            BigDecimal purchase = v.getPurchasePrice() != null ? v.getPurchasePrice() : BigDecimal.ZERO;
            BigDecimal repair = v.getRepairCost() != null ? v.getRepairCost() : BigDecimal.ZERO;
            return "{\"purchasePrice\":" + purchase + ",\"repairCost\":" + repair + ",\"totalInvestment\":"
                    + purchase.add(repair) + "}";
        }
        return "{}";
    }

    // =========================================================================
    // Download: Attendance Report PDF
    // =========================================================================
    @GetMapping("/attendance/download")
    public ResponseEntity<byte[]> downloadAttendanceReport(
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "year",  required = false) Integer year) {

        int m = (month != null) ? month : LocalDate.now().getMonthValue();
        int y = (year  != null) ? year  : LocalDate.now().getYear();

        java.time.YearMonth ym = java.time.YearMonth.of(y, m);
        LocalDate from = ym.atDay(1);
        LocalDate to   = ym.atEndOfMonth();

        List<Attendance> records = attendanceRepository.findByDateBetween(from, to);
        records.sort(Comparator.comparing(Attendance::getDate));

        String period = java.time.Month.of(m).getDisplayName(
                java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH) + " " + y;

        byte[] pdf = pdfReportService.attendanceReport(records, period);
        String filename = "attendance-report-" + y + "-" + String.format("%02d", m) + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
