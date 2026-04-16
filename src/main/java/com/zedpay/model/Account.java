package com.zedpay.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class Account {
    private String id;
    private String accountNumber;
    private double balance;
    private String ownerId;
    private AccountType accountType;
    private List<Map<String, Object>> transactionHistory;

    public Account() {
        this.transactionHistory = new ArrayList<>();
    }

    public Account(String id, String accountNumber, double balance, String ownerId, AccountType accountType) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.ownerId = ownerId;
        this.accountType = accountType;
        this.transactionHistory = new ArrayList<>();
        this.balance = balance;
    }

    public void deposit(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than zero");
        }
        this.balance += amount;
    }

    public abstract boolean withdraw(double amount);

    public abstract String generateStatement();

    public void addTransaction(Map<String, Object> record) {
        if (transactionHistory == null) {
            transactionHistory = new ArrayList<>();
        }
        transactionHistory.add(record);
    }

    // Keeps backward compatibility if any old code still passes plain strings
    public void addTransaction(String record) {
        if (transactionHistory == null) {
            transactionHistory = new ArrayList<>();
        }
        transactionHistory.add(Map.of("record", record));
    }

    public void addTransactionHistory(Map<String, Object> record) {
        addTransaction(record);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public List<Map<String, Object>> getTransactionHistory() {
        return transactionHistory;
    }

    public void setTransactionHistory(List<Map<String, Object>> transactionHistory) {
        this.transactionHistory = transactionHistory;
    }
}