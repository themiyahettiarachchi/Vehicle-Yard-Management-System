package com.vyms;

import com.vyms.entity.Role;
import com.vyms.entity.User;
import com.vyms.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner seedUsers(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() == 0) {
                createUser(userRepository, passwordEncoder, "Admin", "admin@vehicleyard.com", "password", Role.ADMIN);
                createUser(userRepository, passwordEncoder, "Manager", "manager@vehicleyard.com", "password",
                        Role.MANAGER);
                createUser(userRepository, passwordEncoder, "Sales", "sales@vehicleyard.com", "password", Role.SALES);
                createUser(userRepository, passwordEncoder, "Inventory", "inventory@vehicleyard.com", "password",
                        Role.INVENTORY);
                createUser(userRepository, passwordEncoder, "Mechanic", "mechanic@vehicleyard.com", "password",
                        Role.MECHANIC);
                System.out.println(">>> Demo users seeded successfully.");
            } else {
                System.out.println(">>> Users already exist, skipping seed.");
            }
        };
    }

    private void createUser(UserRepository repo, BCryptPasswordEncoder encoder, String username, String email,
            String password, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(encoder.encode(password));
        user.setRole(role);
        repo.save(user);
    }
}
