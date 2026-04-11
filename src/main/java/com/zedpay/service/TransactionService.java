package com.zedpay.service;

import com.zedpay.model.Account;
import com.zedpay.model.MerchantAccount;
import com.zedpay.repository.AccountRepository;
import com.zedpay.transaction.SendMoneyTransaction;
import com.zedpay.transaction.TopUpTransaction;
import com.zedpay.transaction.WithdrawTransaction;

public class TransactionService {
    private final AccountRepository accountRepository;

    public TransactionService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public TopUpTransaction topUp(String accountId, double amount) {
        if (accountId == null || accountId.trim().isEmpty()) {
            throw new IllegalArgumentException("Account ID is required");
        }

        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        Account account = accountRepository.findById(accountId);

        if (account == null) {
            throw new IllegalArgumentException("Account not found");
        }

        TopUpTransaction transaction = new TopUpTransaction(accountId, amount);
        transaction.calculateFee();

        account.deposit(amount);
        transaction.process();
        account.addTransactionHistory(transaction.getSummary());

        accountRepository.save(account);

        return transaction;
    }

    public WithdrawTransaction withdraw(String accountId, double amount) {
        if (accountId == null || accountId.trim().isEmpty()) {
            throw new IllegalArgumentException("Account ID is required");
        }

        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        Account account = accountRepository.findById(accountId);

        if (account == null) {
            throw new IllegalArgumentException("Account not found");
        }

        WithdrawTransaction transaction = new WithdrawTransaction(accountId, amount);
        transaction.calculateFee();

        boolean success = account.withdraw(amount);

        if (!success) {
            throw new IllegalArgumentException("Withdrawal failed: insufficient balance or minimum balance rule violated");
        }

        transaction.process();
        account.addTransactionHistory(transaction.getSummary());

        accountRepository.save(account);

        return transaction;
    }

    public SendMoneyTransaction sendMoney(String fromAccountId, String toAccountId, double amount, boolean merchantPayment) {
        if (fromAccountId == null || fromAccountId.trim().isEmpty()) {
            throw new IllegalArgumentException("Sender account ID is required");
        }

        if (toAccountId == null || toAccountId.trim().isEmpty()) {
            throw new IllegalArgumentException("Receiver account ID is required");
        }

        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Cannot send money to the same account");
        }

        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        Account fromAccount = accountRepository.findById(fromAccountId);
        Account toAccount = accountRepository.findById(toAccountId);

        if (fromAccount == null) {
            throw new IllegalArgumentException("Sender account not found");
        }

        if (toAccount == null) {
            throw new IllegalArgumentException("Receiver account not found");
        }

        if (toAccount instanceof MerchantAccount && !merchantPayment) {
            throw new IllegalArgumentException("Merchant account can only receive money through merchant payment");
        }

        SendMoneyTransaction transaction = new SendMoneyTransaction(fromAccountId, toAccountId, amount);
        double fee = transaction.calculateFee();

        boolean withdrawn = fromAccount.withdraw(amount + fee);

        if (!withdrawn) {
            throw new IllegalArgumentException("Send money failed: insufficient balance or minimum balance rule violated");
        }

        toAccount.deposit(amount);
        transaction.process();

        fromAccount.addTransactionHistory("Sent: " + transaction.getSummary());
        toAccount.addTransactionHistory("Received: " + transaction.getSummary());

        if (fromAccount.getAccountType() != toAccount.getAccountType()) {
            fromAccount.addTransactionHistory("Cross-type fee applied: " + fee);
        }

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        return transaction;
    }
}