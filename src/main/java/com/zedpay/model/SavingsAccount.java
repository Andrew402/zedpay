package com.zedpay.model;

public class SavingsAccount extends Account {
    private double minimumBalance;

    public SavingsAccount() {
        super();
        setAccountType(AccountType.SAVINGS);
    }

    public SavingsAccount(String id, String accountNumber, double balance, String ownerId, double minimumBalance) {
        super(id, accountNumber, balance, ownerId, AccountType.SAVINGS);
        this.minimumBalance = minimumBalance;
    }

    @Override
    public boolean withdraw(double amount) {
        if (amount > 0 && (getBalance() - amount) >= minimumBalance) {
            setBalance(getBalance() - amount);
            return true;
        }
        return false;
    }

    @Override
    public String generateStatement() {
        return "Savings Account Statement - Balance: " + getBalance() +
                ", Minimum Balance: " + minimumBalance;
    }

    public double getMinimumBalance() {
        return minimumBalance;
    }

    public void setMinimumBalance(double minimumBalance) {
        this.minimumBalance = minimumBalance;
    }
}