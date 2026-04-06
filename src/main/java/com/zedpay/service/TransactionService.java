package com.zedpay.service;

import com.zedpay.model.*;
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
        Account account = accountRepository.findById(accountId);
        if (account == null || amount <= 0) {
            return null;
        }

        TopUpTransaction transaction = new TopUpTransaction(accountId, amount);
        transaction.calculateFee();
        account.deposit(amount);
        transaction.process();
        account.addTransactionHistory(transaction.getSummary());

        return transaction;
    }

    public WithdrawTransaction withdraw(String accountId, double amount) {
        Account account = accountRepository.findById(accountId);
        if (account == null || amount <= 0) {
            return null;
        }

        WithdrawTransaction transaction = new WithdrawTransaction(accountId, amount);
        transaction.calculateFee();

        boolean success = account.withdraw(amount);
        if (!success) {
            transaction.getStatus();
            return null;
        }

        transaction.process();
        account.addTransactionHistory(transaction.getSummary());

        return transaction;
    }

    public SendMoneyTransaction sendMoney(String fromAccountId, String toAccountId, double amount, boolean merchantPayment) {
        Account fromAccount = accountRepository.findById(fromAccountId);
        Account toAccount = accountRepository.findById(toAccountId);

        if (fromAccount == null || toAccount == null || amount <= 0) {
            return null;
        }

        if (toAccount instanceof MerchantAccount && !merchantPayment) {
            return null;
        }

        SendMoneyTransaction transaction = new SendMoneyTransaction(fromAccountId, toAccountId, amount);
        double fee = transaction.calculateFee();

        boolean withdrawn = fromAccount.withdraw(amount + fee);
        if (!withdrawn) {
            return null;
        }

        toAccount.deposit(amount);
        transaction.process();

        fromAccount.addTransactionHistory("Sent: " + transaction.getSummary());
        toAccount.addTransactionHistory("Received: " + transaction.getSummary());

        if (fromAccount.getAccountType() != toAccount.getAccountType()) {
            fromAccount.addTransactionHistory("Cross-type fee applied: " + fee);
        }

        return transaction;
    }
}