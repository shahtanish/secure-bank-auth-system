package com.bank.auth.dto;

public class MfaSetupResponse {
    private String secret;
    private String otpAuthUri;
    private String message;

    public MfaSetupResponse() {
    }

    public MfaSetupResponse(String secret, String otpAuthUri, String message) {
        this.secret = secret;
        this.otpAuthUri = otpAuthUri;
        this.message = message;
    }

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public String getOtpAuthUri() { return otpAuthUri; }
    public void setOtpAuthUri(String otpAuthUri) { this.otpAuthUri = otpAuthUri; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public static MfaSetupResponseBuilder builder() {
        return new MfaSetupResponseBuilder();
    }

    public static class MfaSetupResponseBuilder {
        private String secret;
        private String otpAuthUri;
        private String message;

        MfaSetupResponseBuilder() {}

        public MfaSetupResponseBuilder secret(String secret) { this.secret = secret; return this; }
        public MfaSetupResponseBuilder otpAuthUri(String otpAuthUri) { this.otpAuthUri = otpAuthUri; return this; }
        public MfaSetupResponseBuilder message(String message) { this.message = message; return this; }

        public MfaSetupResponse build() {
            return new MfaSetupResponse(secret, otpAuthUri, message);
        }
    }
}
