package com.bank.auth.exception;

import java.util.List;

/**
 * Thrown when a password does not meet the security policy requirements.
 */
public class PasswordPolicyException extends RuntimeException {

    private final List<String> violations;

    public PasswordPolicyException(List<String> violations) {
        super("Password does not meet security requirements");
        this.violations = violations;
    }

    public List<String> getViolations() {
        return violations;
    }
}
