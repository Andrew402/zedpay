package com.zedpay.service;

import com.zedpay.model.StandardAccount;
import com.zedpay.model.User;
import com.zedpay.repository.AccountRepository;
import com.zedpay.repository.UserRepository;

import java.util.ArrayList;
import java.util.UUID;

public class UserService {
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    public UserService(UserRepository userRepository, AccountRepository accountRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
    }

    public User registerUser(String fullName, String phoneNumber, String nationalId, String email, String password) {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Full name is required");
        }

        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number is required");
        }

        if (nationalId == null || nationalId.trim().isEmpty()) {
            throw new IllegalArgumentException("National ID is required");
        }

        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }

        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }

        if (userRepository.existsByEmail(email.trim())) {
            throw new IllegalArgumentException("Email already exists");
        }

        String userId = UUID.randomUUID().toString();
        User user = new User(
                userId,
                fullName.trim(),
                phoneNumber.trim(),
                nationalId.trim(),
                email.trim(),
                password.trim()
        );

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
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID is required");
        }

        User user = userRepository.findById(id.trim());

        if (user == null) {
            return null;
        }

        user.setAccounts(new ArrayList<>(accountRepository.findByOwnerId(user.getId())));
        return user;
    }

    public User loginUser(String email, String password) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }

        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }

        User user = userRepository.findByEmailAndPassword(email.trim(), password.trim());

        if (user == null) {
            return null;
        }

        user.setAccounts(new ArrayList<>(accountRepository.findByOwnerId(user.getId())));
        return user;
    }
}