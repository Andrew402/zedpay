package com.zedpay.service;

import com.zedpay.model.*;
import com.zedpay.repository.AccountRepository;
import com.zedpay.repository.UserRepository;

import java.util.UUID;

public class AccountService {
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public AccountService(AccountRepository accountRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    public Account createAccount(String userId, String accountType, String businessRegistrationId, double minimumBalance) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID is required");
        }

        if (accountType == null || accountType.trim().isEmpty()) {
            throw new IllegalArgumentException("Account type is required");
        }

        if (minimumBalance < 0) {
            throw new IllegalArgumentException("Minimum balance cannot be negative");
        }

        User user = userRepository.findById(userId);

        if (user == null) {
            return null;
        }

        String accountId = UUID.randomUUID().toString();
        String normalizedType = accountType.trim().toUpperCase();
        Account account;

        switch (normalizedType) {
            case "SAVINGS":
                account = new SavingsAccount(
                        accountId,
                        "SAV-" + System.currentTimeMillis(),
                        minimumBalance,
                        userId,
                        minimumBalance
                );
                break;

            case "MERCHANT":
                if (businessRegistrationId == null || businessRegistrationId.trim().isEmpty()) {
                    throw new IllegalArgumentException("Business registration ID is required for merchant account");
                }

                account = new MerchantAccount(
                        accountId,
                        "MER-" + System.currentTimeMillis(),
                        0.0,
                        userId,
                        businessRegistrationId.trim()
                );
                break;

            case "STANDARD":
                account = new StandardAccount(
                        accountId,
                        "STD-" + System.currentTimeMillis(),
                        0.0,
                        userId
                );
                break;

            default:
                throw new IllegalArgumentException("Invalid account type. Use STANDARD, SAVINGS, or MERCHANT");
        }

        user.addAccount(account);
        accountRepository.save(account);

        return account;
    }

    public Account getAccountById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }

        return accountRepository.findById(id);
    }

    public String getStatement(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            return null;
        }

        Account account = accountRepository.findById(accountId);
        if (account == null) {
            return null;
        }

        return account.generateStatement();
    }
}