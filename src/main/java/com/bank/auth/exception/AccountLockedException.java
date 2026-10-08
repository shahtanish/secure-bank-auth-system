package com.bank.auth.exception;

/**
 * Thrown when an account is temporarily or permanently locked
 * due to excessive failed login attempts.
 */
public class AccountLockedException extends RuntimeException {
    public AccountLockedException(String message) {
        super(message);
    }
}
