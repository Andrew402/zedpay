package com.zedpay.service;

import com.zedpay.model.StandardAccount;
import com.zedpay.model.User;
import com.zedpay.repository.AccountRepository;
import com.zedpay.repository.UserRepository;

import java.util.UUID;

public class UserService {
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    public UserService(UserRepository userRepository, AccountRepository accountRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
    }

    public User registerUser(String fullName, String phoneNumber, String nationalId) {
        String userId = UUID.randomUUID().toString();
        User user = new User(userId, fullName, phoneNumber, nationalId);

        String accountId = UUID.randomUUID().toString();
        StandardAccount defaultAccount = new StandardAccount(
                accountId,
                "STD-" + System.currentTimeMillis(),
                0.0,
                userId
        );

        user.addAccount(defaultAccount);

        userRepository.save(user);
        accountRepository.save(defaultAccount);

        return user;
    }

    public User getUserById(String id) {
        return userRepository.findById(id);
    }
}
