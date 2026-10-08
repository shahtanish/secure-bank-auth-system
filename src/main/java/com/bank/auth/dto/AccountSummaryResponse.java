package com.bank.auth.dto;

import java.math.BigDecimal;

public class AccountSummaryResponse {
    private String accountNumber;
    private String maskedAccountNumber;
    private String accountType;
    private String productName;
    private String currency;
    private BigDecimal availableBalance;
    private String status;

    public AccountSummaryResponse() {}

    public AccountSummaryResponse(String accountNumber, String maskedAccountNumber, String accountType, String productName, String currency, BigDecimal availableBalance, String status) {
        this.accountNumber = accountNumber;
        this.maskedAccountNumber = maskedAccountNumber;
        this.accountType = accountType;
        this.productName = productName;
        this.currency = currency;
        this.availableBalance = availableBalance;
        this.status = status;
    }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getMaskedAccountNumber() { return maskedAccountNumber; }
    public void setMaskedAccountNumber(String maskedAccountNumber) { this.maskedAccountNumber = maskedAccountNumber; }
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getAvailableBalance() { return availableBalance; }
    public void setAvailableBalance(BigDecimal availableBalance) { this.availableBalance = availableBalance; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
