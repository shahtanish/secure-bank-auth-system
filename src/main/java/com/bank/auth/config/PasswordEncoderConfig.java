package com.bank.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Password encoder configuration using Argon2id.
 * 
 * WHY ARGON2id OVER BCRYPT:
 * - Argon2id is the winner of the Password Hashing Competition (2015)
 * - It is memory-hard, making GPU/ASIC brute-force attacks exponentially expensive
 * - BCrypt uses ~4KB of memory; our Argon2id config uses 65,536 KB (64 MB)
 * - This means an attacker with a GPU farm would need 64MB per parallel hash attempt
 * - A GPU with 8GB RAM can only try ~125 hashes in parallel vs millions with BCrypt
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(
                16,     // Salt length in bytes
                32,     // Hash length in bytes
                1,      // Parallelism (number of threads)
                65536,  // Memory cost in KB (64 MB) — THIS kills GPU attacks
                3       // Number of iterations
        );
    }
}
