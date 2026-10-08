package com.bank.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequest {

    @NotBlank(message = "Username is required")
    @Size(max = 100, message = "Username too long")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(max = 128, message = "Password too long")
    private String password;

    @Size(min = 6, max = 6, message = "MFA code must be 6 digits")
    private String mfaCode;

    private String deviceFingerprint;

    public LoginRequest() {
    }

    public LoginRequest(String username, String password, String mfaCode, String deviceFingerprint) {
        this.username = username;
        this.password = password;
        this.mfaCode = mfaCode;
        this.deviceFingerprint = deviceFingerprint;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getMfaCode() { return mfaCode; }
    public void setMfaCode(String mfaCode) { this.mfaCode = mfaCode; }

    public String getDeviceFingerprint() { return deviceFingerprint; }
    public void setDeviceFingerprint(String deviceFingerprint) { this.deviceFingerprint = deviceFingerprint; }
}
