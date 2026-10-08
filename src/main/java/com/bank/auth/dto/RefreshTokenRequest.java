package com.bank.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;

    private String deviceFingerprint;

    public RefreshTokenRequest() {
    }

    public RefreshTokenRequest(String refreshToken, String deviceFingerprint) {
        this.refreshToken = refreshToken;
        this.deviceFingerprint = deviceFingerprint;
    }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public String getDeviceFingerprint() { return deviceFingerprint; }
    public void setDeviceFingerprint(String deviceFingerprint) { this.deviceFingerprint = deviceFingerprint; }
}
