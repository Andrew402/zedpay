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
        User user = userRepository.findById(userId);

        if (user == null) {
            return null;
        }

        String accountId = UUID.randomUUID().toString();
        Account account;

        if ("SAVINGS".equalsIgnoreCase(accountType)) {
            account = new SavingsAccount(
                    accountId,
                    "SAV-" + System.currentTimeMillis(),
                    minimumBalance,
                    userId,
                    minimumBalance
            );
        } else if ("MERCHANT".equalsIgnoreCase(accountType)) {
            account = new MerchantAccount(
                    accountId,
                    "MER-" + System.currentTimeMillis(),
                    0.0,
                    userId,
                    businessRegistrationId
            );
        } else {
            account = new StandardAccount(
                    accountId,
                    "STD-" + System.currentTimeMillis(),
                    0.0,
                    userId
            );
        }

        user.addAccount(account);
        accountRepository.save(account);

        return account;
    }

    public Account getAccountById(String id) {
        return accountRepository.findById(id);
    }

    public String getStatement(String accountId) {
        Account account = accountRepository.findById(accountId);
        if (account == null) {
            return null;
        }
        return account.generateStatement();
    }
}
