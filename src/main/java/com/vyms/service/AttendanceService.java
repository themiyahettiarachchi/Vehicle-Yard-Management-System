package com.vyms.service;

import com.vyms.entity.Attendance;
import com.vyms.entity.User;
import com.vyms.repository.AttendanceRepository;
import com.vyms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;

    @Autowired
    public AttendanceService(AttendanceRepository attendanceRepository,
            UserRepository userRepository) {
        this.attendanceRepository = attendanceRepository;
        this.userRepository = userRepository;
    }

    // ─── Basic CRUD ───────────────────────────────────────────────────────────

    public List<Attendance> findAll() {
        return attendanceRepository.findAll();
    }

    public Optional<Attendance> findById(Long id) {
        return attendanceRepository.findById(id);
    }

    public Attendance save(Attendance attendance) {
        return attendanceRepository.save(attendance);
    }

    public void deleteById(Long id) {
        attendanceRepository.deleteById(id);
    }

    // ─── State Machine Actions ────────────────────────────────────────────────

    /**
     * Check-In a worker for today.
     * Rule 1: Fails if a record already exists for this worker today.
     * Rule 3: Fails if today's existing record is OFF_DAY.
     */
    public Attendance checkIn(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        LocalDate today = LocalDate.now();
        Optional<Attendance> existing = attendanceRepository.findByUserAndDate(user, today);

        if (existing.isPresent()) {
            Attendance att = existing.get();
            if ("OFF_DAY".equalsIgnoreCase(att.getStatus())) {
                throw new IllegalStateException("Worker is marked Off Day today. Cannot check in.");
            }
            if ("UNDONE".equalsIgnoreCase(att.getStatus())) {
                att.setStatus("PRESENT");
                att.setCheckInTime(LocalTime.now().truncatedTo(ChronoUnit.MINUTES));
                return attendanceRepository.save(att);
            }
            throw new IllegalStateException("Worker has already checked in today!");
        }

        Attendance att = new Attendance();
        att.setUser(user);
        att.setDate(today);
        att.setCheckInTime(LocalTime.now().truncatedTo(ChronoUnit.MINUTES));
        att.setStatus("PRESENT");
        return attendanceRepository.save(att);
    }

    /**
     * Check-Out a worker for today.
     * Attendance classification rules (applied on checkout):
     *
     *  OFF_DAY  : worked ≤ 60 minutes after check-in (employee left very early)
     *  HALF_DAY : checked out between 13:00 and 13:30 (inclusive)
     *  PRESENT  : worked 8 or more hours  ← full day
     *
     * For anything in between (> 60 min but not a recognised half-day window
     * and < 8 h) we still keep PRESENT — the salary engine will simply not
     * add OT since hours ≤ 8.
     *
     * OT calculation (PERMANENT only) is handled by SalaryCalculationService.
     * CONTRACT workers receive no OT — just the daily wage.
     */
    public Attendance checkOut(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        LocalDate today = LocalDate.now();
        Attendance att = attendanceRepository.findByUserAndDate(user, today)
                .orElseThrow(
                        () -> new IllegalStateException("No check-in record found for today. Please check in first."));

        if (att.getCheckInTime() == null) {
            throw new IllegalStateException("Worker has no check-in time recorded.");
        }
        if (att.getCheckOutTime() != null) {
            throw new IllegalStateException("Worker has already checked out today.");
        }

        LocalTime now = LocalTime.now().truncatedTo(ChronoUnit.MINUTES);

        // Company opens at 9:00 AM. If someone checks in earlier, count working
        // time from 09:00 so we do not over-credit early arrivals.
        LocalTime shiftStart = LocalTime.of(9, 0);
        LocalTime effectiveStart = att.getCheckInTime().isBefore(shiftStart)
                ? shiftStart
                : att.getCheckInTime();

        long minutesWorked = ChronoUnit.MINUTES.between(effectiveStart, now);

        if (minutesWorked < 10) {
            throw new IllegalStateException(
                    "Check-out too soon after check-in! Minimum 10 minutes required. " +
                            "Checked in at " + att.getCheckInTime() + " (" + minutesWorked + " min ago).");
        }

        att.setCheckOutTime(now);

        // ── Classify the day ──────────────────────────────────────────────────
        // Applies to all workers for attendance status; OT pay is only for PERMANENT.
        // Rule 1: worked ≤ 60 minutes → OFF_DAY
        if (minutesWorked <= 60) {
            att.setStatus("OFF_DAY");
        }
        // Rule 2: checkout between 13:00 and 13:30 (inclusive) → HALF_DAY
        else if (!now.isBefore(LocalTime.of(13, 0)) && !now.isAfter(LocalTime.of(13, 30))) {
            att.setStatus("HALF_DAY");
        }
        // Rule 3: worked ≥ 480 minutes (8 hours) → PRESENT (full day, OT calculated later)
        else if (minutesWorked >= 480) {
            att.setStatus("PRESENT");
        }
        // Anything in between (> 1h, not half-day window, < 8h) → PRESENT
        // No OT will be added since hours ≤ 8; not a recognised half-day window.
        else {
            att.setStatus("PRESENT");
        }

        return attendanceRepository.save(att);
    }


    /**
     * Mark a worker as Off Day for today.
     * Fails if any record already exists for this worker today.
     */
    public Attendance markOffDay(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        LocalDate today = LocalDate.now();
        Optional<Attendance> existing = attendanceRepository.findByUserAndDate(user, today);

        if (existing.isPresent()) {
            Attendance att = existing.get();
            if ("UNDONE".equalsIgnoreCase(att.getStatus())) {
                att.setStatus("OFF_DAY");
                return attendanceRepository.save(att);
            }
            throw new IllegalStateException(
                    "A record already exists for this worker today (" +
                            existing.get().getStatus() + "). Cannot mark as Off Day.");
        }

        Attendance att = new Attendance();
        att.setUser(user);
        att.setDate(today);
        att.setStatus("OFF_DAY");
        // checkInTime and checkOutTime remain NULL — Off Day needs no time logs
        return attendanceRepository.save(att);
    }

    /**
     * Clear (delete) today's attendance record for a worker.
     * Allows a manager to undo a mistakenly clicked button.
     */
    public void clearTodayAttendance(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        LocalDate today = LocalDate.now();
        Optional<Attendance> existing = attendanceRepository.findByUserAndDate(user, today);

        if (existing.isPresent()) {
            Attendance att = existing.get();
            if (att.isUndoneToday()) {
                throw new IllegalStateException("Undo action can only be performed once per day.");
            }
            att.setStatus("UNDONE");
            att.setCheckInTime(null);
            att.setCheckOutTime(null);
            att.setUndoneToday(true);
            attendanceRepository.save(att);
        }
    }
}
