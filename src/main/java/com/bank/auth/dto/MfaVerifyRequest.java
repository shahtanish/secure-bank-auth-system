package com.bank.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class MfaVerifyRequest {

    @NotBlank(message = "MFA code is required")
    @Size(min = 6, max = 6, message = "MFA code must be 6 digits")
    private String code;

    public MfaVerifyRequest() {
    }

    public MfaVerifyRequest(String code) {
        this.code = code;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
