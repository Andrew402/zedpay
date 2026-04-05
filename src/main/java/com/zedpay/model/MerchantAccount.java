package com.zedpay.model;

public class MerchantAccount extends Account {
    private String businessRegistrationId;

    public MerchantAccount() {
        super();
        setAccountType(AccountType.MERCHANT);
    }

    public MerchantAccount(String id, String accountNumber, double balance, String ownerId, String businessRegistrationId) {
        super(id, accountNumber, balance, ownerId, AccountType.MERCHANT);
        this.businessRegistrationId = businessRegistrationId;
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
        return "Merchant Account Statement - Balance: " + getBalance() +
                ", Business ID: " + businessRegistrationId;
    }

    public String getBusinessRegistrationId() {
        return businessRegistrationId;
    }

    public void setBusinessRegistrationId(String businessRegistrationId) {
        this.businessRegistrationId = businessRegistrationId;
    }
}