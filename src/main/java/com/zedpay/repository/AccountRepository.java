package com.zedpay.repository;

import com.zedpay.model.Account;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AccountRepository {
    private final Map<String, Account> accounts = new HashMap<>();

    public void save(Account account) {
        accounts.put(account.getId(), account);
    }

    public Account findById(String id) {
        return accounts.get(id);
    }

    public boolean existsById(String id) {
        return accounts.containsKey(id);
    }

    public Collection<Account> findAll() {
        return accounts.values();
    }
}