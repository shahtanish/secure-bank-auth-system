package com.bank.auth.exception;

/**
 * Thrown when MFA verification is required to complete login.
 * This is NOT an error — it's a partial success indicating
 * the user must provide their TOTP code.
 */
public class MfaRequiredException extends RuntimeException {
    public MfaRequiredException(String message) {
        super(message);
    }
}
