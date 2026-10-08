package com.bank.auth.controller;

import com.bank.auth.dto.DashboardResponse;
import com.bank.auth.entity.User;
import com.bank.auth.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public ResponseEntity<DashboardResponse> getAccountDashboard(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "false") boolean includeClosed) {

        // Use the CIF securely injected from the JWT token via Spring Security context
        DashboardResponse response = accountService.getAccountDashboard(user.getCif(), type, includeClosed);
        return ResponseEntity.ok(response);
    }
}
