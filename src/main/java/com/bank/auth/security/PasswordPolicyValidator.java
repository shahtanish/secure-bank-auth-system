package com.bank.auth.security;

import com.bank.auth.exception.PasswordPolicyException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * World-bank level password policy enforcer.
 */
@Component
public class PasswordPolicyValidator {

    private static final int MIN_LENGTH = 12;
    private static final int MAX_LENGTH = 128;

    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "password123!", "Password123!", "Admin@123456", "Qwerty@12345",
            "Welcome@1234", "Passw0rd1234!", "Change_me1234", "WorldBank@123"
    );

    public void validate(String password) {
        List<String> violations = new ArrayList<>();

        if (password == null || password.length() < MIN_LENGTH) {
            violations.add("Password must be at least " + MIN_LENGTH + " characters long");
        }
        if (password != null && password.length() > MAX_LENGTH) {
            violations.add("Password must not exceed " + MAX_LENGTH + " characters");
        }
        if (password != null && !password.matches(".*[A-Z].*")) {
            violations.add("Password must contain at least one uppercase letter");
        }
        if (password != null && !password.matches(".*[a-z].*")) {
            violations.add("Password must contain at least one lowercase letter");
        }
        if (password != null && !password.matches(".*\\d.*")) {
            violations.add("Password must contain at least one numeric digit");
        }
        if (password != null && !password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            violations.add("Password must contain at least one special character");
        }
        if (password != null && COMMON_PASSWORDS.contains(password.trim())) {
            violations.add("This password is on the list of known weak/compromised passwords");
        }
        if (password != null && password.matches(".*(.)\\1{2,}.*")) {
            violations.add("Password must not contain more than 2 consecutive identical characters");
        }

        if (!violations.isEmpty()) {
            throw new PasswordPolicyException(violations);
        }
    }
}
