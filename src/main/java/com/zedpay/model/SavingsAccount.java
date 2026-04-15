package com.zedpay.model;

public class SavingsAccount extends Account {
    private static final double DEFAULT_MINIMUM_BALANCE = 100.0;
    private double minimumBalance;

    public SavingsAccount() {
        super();
        setAccountType(AccountType.SAVINGS);
        this.minimumBalance = DEFAULT_MINIMUM_BALANCE;
    }

    public SavingsAccount(String id, String accountNumber, double balance, String ownerId, double minimumBalance) {
        super(id, accountNumber, balance, ownerId, AccountType.SAVINGS);
        this.minimumBalance = Math.max(0.0, minimumBalance);
    }

    @Override
    public boolean withdraw(double amount) {
        if (amount <= 0) {
            return false;
        }

        double remainingBalance = getBalance() - amount;

        if (remainingBalance >= minimumBalance) {
            setBalance(remainingBalance);
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
        if (minimumBalance >= 0) {
            this.minimumBalance = minimumBalance;
        }
    }
}