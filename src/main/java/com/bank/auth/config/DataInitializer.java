package com.bank.auth.config;

import com.bank.auth.entity.Role;
import com.bank.auth.repository.UserRepository;
import com.bank.auth.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Data Initializer to seed default accounts into H2 / PostgreSQL.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final AuthService authService;

    public DataInitializer(UserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            log.info("Seeding initial Bank Security Accounts with Argon2id passwords...");

            // Create Admin User
            authService.registerUser(
                    "CIF_ADMIN_01",
                    "admin",
                    "BankAdmin#2026!",
                    "admin@worldbank.org",
                    Role.ADMIN
            );

            // Create Standard Bank Customer User
            authService.registerUser(
                    "CIF_USER_02",
                    "bankuser",
                    "UserSecure#2026!",
                    "customer@worldbank.org",
                    Role.USER
            );

            log.info("==========================================================");
            log.info("DEMO ACCOUNTS CREATED SUCCESSFULLY:");
            log.info("1. Admin User -> username: admin | password: BankAdmin#2026!");
            log.info("2. Customer   -> username: bankuser | password: UserSecure#2026!");
            log.info("==========================================================");
        }
    }
}
