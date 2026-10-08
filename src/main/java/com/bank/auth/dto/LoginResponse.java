package com.bank.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private long expiresIn;
    private boolean mfaEnabled;
    private boolean mfaRequired;
    private String message;

    public LoginResponse() {
    }

    public LoginResponse(String accessToken, String refreshToken, String tokenType, long expiresIn,
                         boolean mfaEnabled, boolean mfaRequired, String message) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType != null ? tokenType : "Bearer";
        this.expiresIn = expiresIn;
        this.mfaEnabled = mfaEnabled;
        this.mfaRequired = mfaRequired;
        this.message = message;
    }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }

    public boolean isMfaEnabled() { return mfaEnabled; }
    public void setMfaEnabled(boolean mfaEnabled) { this.mfaEnabled = mfaEnabled; }

    public boolean isMfaRequired() { return mfaRequired; }
    public void setMfaRequired(boolean mfaRequired) { this.mfaRequired = mfaRequired; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public static LoginResponseBuilder builder() {
        return new LoginResponseBuilder();
    }

    public static class LoginResponseBuilder {
        private String accessToken;
        private String refreshToken;
        private String tokenType = "Bearer";
        private long expiresIn;
        private boolean mfaEnabled;
        private boolean mfaRequired;
        private String message;

        LoginResponseBuilder() {}

        public LoginResponseBuilder accessToken(String accessToken) { this.accessToken = accessToken; return this; }
        public LoginResponseBuilder refreshToken(String refreshToken) { this.refreshToken = refreshToken; return this; }
        public LoginResponseBuilder tokenType(String tokenType) { this.tokenType = tokenType; return this; }
        public LoginResponseBuilder expiresIn(long expiresIn) { this.expiresIn = expiresIn; return this; }
        public LoginResponseBuilder mfaEnabled(boolean mfaEnabled) { this.mfaEnabled = mfaEnabled; return this; }
        public LoginResponseBuilder mfaRequired(boolean mfaRequired) { this.mfaRequired = mfaRequired; return this; }
        public LoginResponseBuilder message(String message) { this.message = message; return this; }

        public LoginResponse build() {
            return new LoginResponse(accessToken, refreshToken, tokenType, expiresIn, mfaEnabled, mfaRequired, message);
        }
    }
}
