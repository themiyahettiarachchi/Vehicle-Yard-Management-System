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
     * Requires a check-in record to exist with no check-out yet.
     * Rule 2: Fails if check-out is less than 10 minutes after check-in.
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
        long minutesDiff = ChronoUnit.MINUTES.between(att.getCheckInTime(), now);
        if (minutesDiff < 10) {
            throw new IllegalStateException(
                    "Check-out too soon after check-in! Minimum 10 minutes required. " +
                            "Checked in at " + att.getCheckInTime() + " (" + minutesDiff + " min ago).");
        }

        att.setCheckOutTime(now);

        // Auto-detect HALF_DAY: less than 4 hours worked
        if (minutesDiff < 240) {
            att.setStatus("HALF_DAY");
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
