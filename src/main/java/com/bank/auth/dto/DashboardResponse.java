package com.bank.auth.dto;

import java.util.List;

public class DashboardResponse {
    private String cif;
    private String customerName;
    private int totalAccounts;
    private List<AccountSummaryResponse> accounts;

    public DashboardResponse() {}

    public DashboardResponse(String cif, String customerName, int totalAccounts, List<AccountSummaryResponse> accounts) {
        this.cif = cif;
        this.customerName = customerName;
        this.totalAccounts = totalAccounts;
        this.accounts = accounts;
    }

    public String getCif() { return cif; }
    public void setCif(String cif) { this.cif = cif; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public int getTotalAccounts() { return totalAccounts; }
    public void setTotalAccounts(int totalAccounts) { this.totalAccounts = totalAccounts; }
    public List<AccountSummaryResponse> getAccounts() { return accounts; }
    public void setAccounts(List<AccountSummaryResponse> accounts) { this.accounts = accounts; }
}
