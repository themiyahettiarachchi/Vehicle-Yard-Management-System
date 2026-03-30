package com.vyms.repository;

import com.vyms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // find by username later
    Optional<User> findByUsername(String username);

    // ADD THIS LINE: This allows login via the Email field in your UI
    Optional<User> findByEmail(String email);
}