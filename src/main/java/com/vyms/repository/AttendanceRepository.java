package com.vyms.repository;

import com.vyms.entity.Attendance;
import com.vyms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByDate(LocalDate date);

    Optional<Attendance> findByUserAndDate(User user, LocalDate date);

    List<Attendance> findByUserAndDateBetween(User user, LocalDate start, LocalDate end);

    List<Attendance> findByDateBetween(LocalDate start, LocalDate end);
}
