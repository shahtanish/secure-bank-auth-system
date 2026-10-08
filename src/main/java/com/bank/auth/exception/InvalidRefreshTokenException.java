package com.bank.auth.exception;

/**
 * Thrown when a refresh token is invalid, expired, revoked,
 * or when token theft is detected.
 */
public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
