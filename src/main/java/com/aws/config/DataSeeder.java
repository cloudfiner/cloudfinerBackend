package com.aws.config;

import com.aws.entity.Role;
import com.aws.entity.User;
import com.aws.repository.RoleRepository;
import com.aws.repository.UserRepository;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class DataSeeder {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() {

        // ✅ Create roles
        String[] roleNames = {"ROLE_ADMIN", "ROLE_USER"};

        for (String roleName : roleNames) {
            roleRepository.findByName(roleName)
                    .orElseGet(() -> roleRepository.save(new Role(roleName)));
        }

        // ✅ Create default admin
        if (!userRepository.existsByEmail("admin@cloudfiner.com")) {

            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseThrow(() -> new RuntimeException("ROLE_ADMIN not found"));

            Set<Role> roles = new HashSet<>();
            roles.add(adminRole);

            User admin = new User();
            admin.setName("Admin");
            admin.setEmail("admin@cloudfiner.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRoles(roles);

            userRepository.save(admin);

            System.out.println("Admin created: admin@cloudfiner.com / admin123");
        }

        // ✅ Create normal user
        if (!userRepository.existsByEmail("user@cloudfiner.com")) {

            Role userRole = roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new RuntimeException("ROLE_USER not found"));

            Set<Role> roles = new HashSet<>();
            roles.add(userRole);

            User user = new User();
            user.setName("User");
            user.setEmail("user@cloudfiner.com");
            user.setPassword(passwordEncoder.encode("user123"));
            user.setRoles(roles);

            userRepository.save(user);

            System.out.println("User created: user@cloudfiner.com / user123");
        }
    }
}