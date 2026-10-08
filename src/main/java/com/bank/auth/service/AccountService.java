package com.bank.auth.service;

import com.bank.auth.dto.AccountSummaryResponse;
import com.bank.auth.dto.DashboardResponse;
import com.bank.auth.entity.Account;
import com.bank.auth.enums.AccountType;
import com.bank.auth.repository.AccountRepository;
import com.bank.auth.stub.CoreBankingStubService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;
    private final CoreBankingStubService stubService;

    public AccountService(AccountRepository accountRepository, CoreBankingStubService stubService) {
        this.accountRepository = accountRepository;
        this.stubService = stubService;
    }

    public DashboardResponse getAccountDashboard(String cif, String type, boolean includeClosed) {
        // 1. Fetch from Stub (Core Banking Simulation)
        List<Account> stubAccounts = stubService.fetchAccountsFromCore(cif);
        
        // 2. Fetch from DB
        List<Account> dbAccounts = accountRepository.findByCif(cif);
        
        // 3. Diff and Sync Logic
        syncAccounts(stubAccounts, dbAccounts);
        
        // 4. Fetch the newly synced data from DB to serve the request
        List<Account> syncedAccounts = accountRepository.findByCif(cif);
        
        // 5. Apply filters (type, includeClosed)
        List<AccountSummaryResponse> filteredAccounts = syncedAccounts.stream()
            .filter(acc -> includeClosed || !"CLOSED".equals(acc.getStatus().name()))
            .filter(acc -> {
                if (type == null || type.isBlank()) return true;
                return acc.getAccountType().name().equalsIgnoreCase(type);
            })
            .sorted(Comparator.comparing((Account a) -> a.getAccountType().name()).reversed() // SAVINGS before CURRENT
                    .thenComparing(Account::getAccountNumber))
            .map(this::mapToSummary)
            .collect(Collectors.toList());

        String customerName = syncedAccounts.isEmpty() ? "Unknown" : syncedAccounts.get(0).getAccountHolderName();

        return new DashboardResponse(cif, customerName, filteredAccounts.size(), filteredAccounts);
    }

    private void syncAccounts(List<Account> stubAccounts, List<Account> dbAccounts) {
        Map<String, Account> dbAccountMap = dbAccounts.stream()
                .collect(Collectors.toMap(Account::getAccountNumber, Function.identity()));

        for (Account stubAcc : stubAccounts) {
            Account dbAcc = dbAccountMap.get(stubAcc.getAccountNumber());
            if (dbAcc == null) {
                // Not in DB -> Create (Sync First Time / New Account)
                log.info("Syncing new account from core banking for CIF: {}, Account: {}", stubAcc.getCif(), mask(stubAcc.getAccountNumber()));
                accountRepository.save(stubAcc);
            } else {
                // In DB -> Diff each field to see if update is needed
                boolean changed = false;

                if (stubAcc.getAvailableBalance().compareTo(dbAcc.getAvailableBalance()) != 0) {
                    dbAcc.setAvailableBalance(stubAcc.getAvailableBalance());
                    changed = true;
                }
                if (stubAcc.getLedgerBalance().compareTo(dbAcc.getLedgerBalance()) != 0) {
                    dbAcc.setLedgerBalance(stubAcc.getLedgerBalance());
                    changed = true;
                }
                if (!stubAcc.getStatus().equals(dbAcc.getStatus())) {
                    dbAcc.setStatus(stubAcc.getStatus());
                    changed = true;
                }
                // Add more field comparisons as needed (productName, etc.)

                if (changed) {
                    log.info("Changes detected for account {}. Updating DB.", mask(dbAcc.getAccountNumber()));
                    // The @Version field on the Account entity will automatically increment on save
                    accountRepository.save(dbAcc);
                }
            }
        }
    }

    private AccountSummaryResponse mapToSummary(Account acc) {
        return new AccountSummaryResponse(
                acc.getAccountNumber(),
                mask(acc.getAccountNumber()),
                acc.getAccountType().name(),
                acc.getProductName(),
                acc.getCurrency(),
                acc.getAvailableBalance(),
                acc.getStatus().name()
        );
    }

    private String mask(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 10) return accountNumber;
        String first6 = accountNumber.substring(0, 6);
        String last3 = accountNumber.substring(accountNumber.length() - 3);
        String masks = "*".repeat(accountNumber.length() - 9);
        return first6 + masks + last3;
    }
}
