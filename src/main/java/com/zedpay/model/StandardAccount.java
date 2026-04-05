package com.zedpay.model;

public class StandardAccount extends Account {

    public StandardAccount() {
        super();
        setAccountType(AccountType.STANDARD);
    }

    public StandardAccount(String id, String accountNumber, double balance, String ownerId) {
        super(id, accountNumber, balance, ownerId, AccountType.STANDARD);
    }

    @Override
    public boolean withdraw(double amount) {
        if (amount > 0 && amount <= getBalance()) {
            setBalance(getBalance() - amount);
            return true;
        }
        return false;
    }

    @Override
    public String generateStatement() {
        return "Standard Account Statement - Balance: " + getBalance();
    }
}