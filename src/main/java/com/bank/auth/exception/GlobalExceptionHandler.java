package com.bank.auth.exception;

import com.bank.auth.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler.
 * 
 * CRITICAL SECURITY RULE: 
 * NEVER expose stack traces, internal class names, or specific error details
 * in the response body. All errors return generic, safe messages.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        // NEVER differentiate between "user not found" and "wrong password"
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid credentials"));
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountLocked(AccountLockedException ex) {
        return ResponseEntity.status(HttpStatus.LOCKED) // 423
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MfaRequiredException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleMfaRequired(MfaRequiredException ex) {
        Map<String, Object> data = new HashMap<>();
        data.put("mfaRequired", true);
        return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED) // 428
                .body(ApiResponse.success("MFA verification required", data));
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Session expired. Please login again."));
    }

    @ExceptionHandler(CredentialsExpiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleExpiredCredentials(CredentialsExpiredException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Password expired. Please reset your password."));
    }

    @ExceptionHandler(PasswordPolicyException.class)
    public ResponseEntity<ApiResponse<Object>> handlePasswordPolicy(PasswordPolicyException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.success("Password does not meet requirements", 
                    ex.getViolations()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.badRequest()
                .body(ApiResponse.success("Validation failed", errors));
    }

    /**
     * Catch-all: NEVER expose internal exception details.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        // Log the full stack trace internally
        log.error("Unexpected error occurred", ex);
        // Return a safe, generic message to the client
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred"));
    }
}
