package com.greenforce.config;

import com.greenforce.entity.Role;
import com.greenforce.entity.User;
import com.greenforce.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/** Creates the first admin account on startup (so the admin endpoints are usable). */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String DEFAULT_PASSWORD = "Admin@12345";

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String adminName;
    private final String adminEmail;
    private final String adminPassword;

    public DataSeeder(UserRepository users, PasswordEncoder encoder,
                      @Value("${app.admin.name}") String adminName,
                      @Value("${app.admin.email}") String adminEmail,
                      @Value("${app.admin.password}") String adminPassword) {
        this.users = users;
        this.encoder = encoder;
        this.adminName = adminName;
        this.adminEmail = adminEmail.trim().toLowerCase(Locale.ROOT);
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (users.existsByEmail(adminEmail)) {
            return;
        }
        users.save(new User(adminName, adminEmail, encoder.encode(adminPassword), Role.ADMIN));
        log.info("Created admin account {}", adminEmail);
        if (DEFAULT_PASSWORD.equals(adminPassword)) {
            log.warn("Admin is using the DEFAULT password. Set the ADMIN_PASSWORD environment variable before deploying!");
        }
    }
}
