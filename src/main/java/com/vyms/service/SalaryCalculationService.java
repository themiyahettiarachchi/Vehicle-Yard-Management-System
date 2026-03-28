package com.vyms.service;

import com.vyms.entity.*;
import com.vyms.entity.Role;
import com.vyms.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 3-Phase payroll engine (revised).
 *
 * Phase 1 – Base Pay
 * CONTRACT : presentDays * dailyWage (HALF_DAY counts as 0.5)
 * PERMANENT: basicSalary − (basicSalary/30 * absentDays)
 *
 * Phase 2 – OT (PERMANENT workers only, no commissions)
 * OT hours : hours beyond 8 h per PRESENT day * otHourlyRate
 * CONTRACT workers receive no OT.
 *
 * Phase 3 – EPF / ETF (PERMANENT only, based on basicSalary)
 * Employee EPF : 8% Company EPF : 12% Company ETF : 3%
 *
 * Phase 4 – Net Salary
 * CONTRACT : grossSalary (no deductions)
 * PERMANENT: grossSalary − epfEmployee
 */
@Service
public class SalaryCalculationService {

    private static final BigDecimal THIRTY = BigDecimal.valueOf(30);
    private static final BigDecimal STANDARD_HOURS = BigDecimal.valueOf(8);
    private static final BigDecimal HALF = new BigDecimal("0.5");
    private static final BigDecimal EPF_EMP_RATE = new BigDecimal("0.08");
    private static final BigDecimal EPF_CO_RATE = new BigDecimal("0.12");
    private static final BigDecimal ETF_CO_RATE = new BigDecimal("0.03");
    private static final BigDecimal SIXTY = BigDecimal.valueOf(60);
    private static final int OFF_DAY_ALLOWANCE = 5; // free off-days for PERMANENT workers per month

    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;
    private final PayrollRepository payrollRepository;

    @Autowired
    public SalaryCalculationService(UserRepository userRepository,
            AttendanceRepository attendanceRepository,
            PayrollRepository payrollRepository) {
        this.userRepository = userRepository;
        this.attendanceRepository = attendanceRepository;
        this.payrollRepository = payrollRepository;
    }

    // =========================================================================
    // Public API
    // =========================================================================

    /** Generate (or overwrite) payroll for all non-ADMIN staff for month/year. */
    public List<Payroll> generateMonthlyPayroll(int month, int year) {
        LocalDate periodStart = LocalDate.of(year, month, 1);
        LocalDate periodEnd = YearMonth.of(year, month).atEndOfMonth();

        List<User> staff = userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && u.getRole() != Role.ADMIN)
                .toList();

        List<Payroll> results = new ArrayList<>();
        for (User u : staff) {
            results.add(calculateForUser(u, month, year, periodStart, periodEnd));
        }
        return results;
    }

    /** Calculate payroll for a single user in the given month. */
    public Payroll calculateForUser(User u, int month, int year,
            LocalDate periodStart, LocalDate periodEnd) {

        Payroll p = payrollRepository.findByUserAndMonthAndYear(u, month, year)
                .orElse(new Payroll());
        p.setUser(u);
        p.setMonth(month);
        p.setYear(year);

        String type = u.getContractType() != null ? u.getContractType().toUpperCase() : "PERMANENT";
        p.setEmploymentType(type);

        List<Attendance> records = attendanceRepository.findByUserAndDateBetween(u, periodStart, periodEnd);

        // Count day types
        long presentDays = records.stream()
                .filter(a -> "PRESENT".equalsIgnoreCase(a.getStatus())).count();
        long halfDays = records.stream()
                .filter(a -> "HALF_DAY".equalsIgnoreCase(a.getStatus())).count();
        long absentDays = records.stream()
                .filter(a -> "ABSENT".equalsIgnoreCase(a.getStatus())).count();
        long offDays = records.stream()
                .filter(a -> "OFF_DAY".equalsIgnoreCase(a.getStatus())).count();

        // Store attendance summary on payroll
        p.setPresentDays((int) presentDays);
        p.setHalfDays((int) halfDays);
        p.setAbsentDays((int) absentDays);
        p.setOffDays((int) offDays);

        // ── Phase 1: Base Pay ─────────────────────────────────────────────────
        BigDecimal basePay = BigDecimal.ZERO;
        BigDecimal leaveDeduction = BigDecimal.ZERO;
        BigDecimal basicSalary = nvl(u.getSalaryRate());
        int unpaidOffDays = 0;

        if ("CONTRACT".equals(type)) {
            BigDecimal daily = nvl(u.getDailyWage());
            // Full days + half days at 0.5
            BigDecimal paidDays = BigDecimal.valueOf(presentDays)
                    .add(HALF.multiply(BigDecimal.valueOf(halfDays)));
            basePay = daily.multiply(paidDays);
        } else {
            // PERMANENT: full month salary − deductions for absences
            // Half-days count as 0.5 absent (deduct half a daily rate)
            basePay = basicSalary;
            BigDecimal dailyRate = basicSalary.compareTo(BigDecimal.ZERO) > 0
                    ? basicSalary.divide(THIRTY, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal absentDeduct = dailyRate.multiply(BigDecimal.valueOf(absentDays));
            BigDecimal halfDayDeduct = dailyRate.multiply(HALF).multiply(BigDecimal.valueOf(halfDays));
            // First 5 OFF_DAY entries are free; beyond that, treat as unpaid days
            unpaidOffDays = (int) Math.max(0, offDays - OFF_DAY_ALLOWANCE);
            BigDecimal extraOffDeduct = dailyRate.multiply(BigDecimal.valueOf(unpaidOffDays));

            leaveDeduction = absentDeduct
                    .add(halfDayDeduct)
                    .add(extraOffDeduct);
        }

        BigDecimal adjustedBase = basePay.subtract(leaveDeduction).max(BigDecimal.ZERO);

        p.setBasePay(fmt(basePay));
        p.setLeaveDeduction(fmt(leaveDeduction));
        p.setAdjustedBasePay(fmt(adjustedBase));
        p.setUnpaidOffDays(unpaidOffDays);

        // ── Phase 2: OT (PERMANENT workers only) ─────────────────────────────
        BigDecimal totalOtHours = BigDecimal.ZERO;
        if ("PERMANENT".equals(type)) {
            for (Attendance att : records) {
                if ("PRESENT".equalsIgnoreCase(att.getStatus())
                        && att.getCheckInTime() != null && att.getCheckOutTime() != null) {
                    long minutesWorked = ChronoUnit.MINUTES.between(att.getCheckInTime(), att.getCheckOutTime());
                    if (minutesWorked > 0) {
                        BigDecimal hoursWorked = BigDecimal.valueOf(minutesWorked)
                                .divide(SIXTY, 4, RoundingMode.HALF_UP);
                        BigDecimal ot = hoursWorked.subtract(STANDARD_HOURS).max(BigDecimal.ZERO);
                        totalOtHours = totalOtHours.add(ot);
                    }
                }
            }
        }
        BigDecimal otRate = nvl(u.getOtHourlyRate());
        BigDecimal otPay = totalOtHours.multiply(otRate);

        p.setOtHours(fmt(totalOtHours));
        p.setOtPay(fmt(otPay));

        // No commissions — always zero
        p.setCommissionPay(BigDecimal.ZERO.setScale(2));

        BigDecimal grossSalary = adjustedBase.add(otPay);
        p.setGrossSalary(fmt(grossSalary));

        // ── Phase 3: EPF / ETF (PERMANENT only) ──────────────────────────────
        BigDecimal epfEmployee = BigDecimal.ZERO;
        BigDecimal companyEpf = BigDecimal.ZERO;
        BigDecimal companyEtf = BigDecimal.ZERO;

        if ("PERMANENT".equals(type) && basicSalary.compareTo(BigDecimal.ZERO) > 0) {
            epfEmployee = basicSalary.multiply(EPF_EMP_RATE);
            companyEpf = basicSalary.multiply(EPF_CO_RATE);
            companyEtf = basicSalary.multiply(ETF_CO_RATE);
        }
        p.setEpfEmployee(fmt(epfEmployee));
        p.setCompanyEpf(fmt(companyEpf));
        p.setCompanyEtf(fmt(companyEtf));

        // ── Phase 4: Net Salary ───────────────────────────────────────────────
        BigDecimal netSalary = grossSalary.subtract(epfEmployee).max(BigDecimal.ZERO);
        p.setNetSalary(fmt(netSalary));

        return payrollRepository.save(p);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private BigDecimal fmt(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
