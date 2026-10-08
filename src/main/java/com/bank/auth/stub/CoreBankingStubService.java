package com.bank.auth.stub;

import com.bank.auth.entity.Account;
import com.bank.auth.enums.AccountStatus;
import com.bank.auth.enums.AccountType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Simulates calling an external Core Banking System API.
 * In a real bank, this would make an HTTP/gRPC call.
 */
@Service
public class CoreBankingStubService {

    public List<Account> fetchAccountsFromCore(String cif) {
        List<Account> accounts = new ArrayList<>();

        if ("CIF_ADMIN_01".equals(cif)) {
            accounts.add(buildAccount(cif, "1501201234567", "Admin User", AccountType.SAVINGS, "Staff Savings", "USD", new BigDecimal("50000.00"), new BigDecimal("50000.00"), AccountStatus.ACTIVE));
        } else if ("CIF_USER_02".equals(cif)) {
            // Simulating an update from the Core Banking system:
            // 1. Spent some money from SAVINGS (Balance dropped from 125,430.50 -> 80,000.00), and status changed to INACTIVE
            accounts.add(buildAccount(cif, "1501209876543", "Bank User", AccountType.SAVINGS, "Regular Savings", "BDT", new BigDecimal("80000.00"), new BigDecimal("81000.00"), AccountStatus.INACTIVE));
            
            // 2. The CURRENT account was marked as DORMANT due to inactivity
            accounts.add(buildAccount(cif, "1501209876544", "Bank User", AccountType.CURRENT, "USD Current", "USD", new BigDecimal("2300.00"), new BigDecimal("2300.00"), AccountStatus.DORMANT));
        }
        
        return accounts;
    }

    private Account buildAccount(String cif, String accountNumber, String holderName, AccountType type, String product, String currency, BigDecimal available, BigDecimal ledger, AccountStatus status) {
        Account acc = new Account();
        acc.setCif(cif);
        acc.setAccountNumber(accountNumber);
        acc.setAccountHolderName(holderName);
        acc.setAccountType(type);
        acc.setProductName(product);
        acc.setCurrency(currency);
        acc.setBranchCode("0150");
        acc.setBranchName("Gulshan Branch");
        acc.setAvailableBalance(available);
        acc.setLedgerBalance(ledger);
        acc.setStatus(status);
        acc.setOpenedOn(LocalDate.of(2019, 3, 14));
        return acc;
    }
}
