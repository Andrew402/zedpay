package com.zedpay.model;

import java.util.ArrayList;
import java.util.List;

public abstract class Account {
    private String id;
    private String accountNumber;
    private double balance;
    private String ownerId;
    private AccountType accountType;
    private List<String> transactionHistory;

    public Account() {
        this.transactionHistory = new ArrayList<>();
    }

    public Account(String id, String accountNumber, double balance, String ownerId, AccountType accountType) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.ownerId = ownerId;
        this.accountType = accountType;
        this.transactionHistory = new ArrayList<>();
        setBalance(balance);
    }

    public void deposit(double amount) {
        if (amount > 0) {
            this.balance += amount;
        }
    }

    public abstract boolean withdraw(double amount);

    public abstract String generateStatement();

    public void addTransactionHistory(String record) {
        transactionHistory.add(record);
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
        if (balance >= 0) {
            this.balance = balance;
        }
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

    public List<String> getTransactionHistory() {
        return transactionHistory;
    }

    public void setTransactionHistory(List<String> transactionHistory) {
        this.transactionHistory = transactionHistory;
    }
}