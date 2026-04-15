package com.zedpay.service;

import com.zedpay.model.Account;
import com.zedpay.model.LedgerEntry;
import com.zedpay.model.MerchantAccount;
import com.zedpay.model.SavingsAccount;
import com.zedpay.repository.AccountRepository;
import com.zedpay.transaction.SendMoneyTransaction;
import com.zedpay.transaction.TopUpTransaction;
import com.zedpay.transaction.WithdrawTransaction;

public class TransactionService {
    private final AccountRepository accountRepository;
    private final FraudDetector fraudDetector;
    private final Ledger ledger;

    public TransactionService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
        this.fraudDetector = new FraudDetector();
        this.ledger = Ledger.getInstance();
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
        transaction.process();

        account.setBalance(account.getBalance() + amount);

        account.addTransaction(transaction.getSummary());

        accountRepository.update(account);

        ledger.recordEntry(new LedgerEntry(
                "TOPUP",
                accountId,
                null,
                amount,
                0.0,
                "Top up into account " + accountId
        ));

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
        double fee = transaction.calculateFee();
        double totalDebit = amount + fee;

        if (account instanceof SavingsAccount savingsAccount) {
            if ((account.getBalance() - totalDebit) < savingsAccount.getMinimumBalance()) {
                throw new IllegalArgumentException("Savings account cannot go below minimum balance");
            }
        } else {
            if (account.getBalance() < totalDebit) {
                throw new IllegalArgumentException("Insufficient balance");
            }
        }

        transaction.process();

        account.setBalance(account.getBalance() - totalDebit);

        account.addTransaction(transaction.getSummary());

        accountRepository.update(account);

        ledger.recordEntry(new LedgerEntry(
                "WITHDRAW",
                accountId,
                null,
                0.0,
                totalDebit,
                "Withdrawal from account " + accountId
        ));

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
        if (fromAccount == null) {
            throw new IllegalArgumentException("Sender account not found");
        }

        Account toAccount = accountRepository.findById(toAccountId);
        if (toAccount == null) {
            throw new IllegalArgumentException("Receiver account not found");
        }

        // Enforce merchant rule:
        // Merchant accounts can only receive transfers marked as merchant payments
        if (toAccount instanceof MerchantAccount && !merchantPayment) {
            throw new IllegalArgumentException("Merchant accounts can only receive merchant payments");
        }

        SendMoneyTransaction transaction = new SendMoneyTransaction(fromAccountId, toAccountId, amount);
        double fee = transaction.calculateFee();
        double totalDebit = amount + fee;

        if (fromAccount instanceof SavingsAccount savingsAccount) {
            if ((fromAccount.getBalance() - totalDebit) < savingsAccount.getMinimumBalance()) {
                throw new IllegalArgumentException("Savings account cannot go below minimum balance");
            }
        } else {
            if (fromAccount.getBalance() < totalDebit) {
                throw new IllegalArgumentException("Insufficient balance");
            }
        }

        boolean suspicious = fraudDetector.recordIncomingTransaction(toAccountId);

        transaction.process();

        fromAccount.setBalance(fromAccount.getBalance() - totalDebit);
        toAccount.setBalance(toAccount.getBalance() + amount);

        fromAccount.addTransaction(transaction.getSummary());
        toAccount.addTransaction(transaction.getSummary());

        accountRepository.update(fromAccount);
        accountRepository.update(toAccount);

        ledger.recordEntry(new LedgerEntry(
                merchantPayment ? "MERCHANT_PAYMENT" : "SEND_MONEY",
                fromAccountId,
                toAccountId,
                0.0,
                totalDebit,
                "Money sent from " + fromAccountId + " to " + toAccountId
        ));

        ledger.recordEntry(new LedgerEntry(
                merchantPayment ? "MERCHANT_PAYMENT" : "SEND_MONEY",
                toAccountId,
                fromAccountId,
                amount,
                0.0,
                "Money received by " + toAccountId + " from " + fromAccountId
        ));

        if (suspicious) {
            ledger.recordEntry(new LedgerEntry(
                    "FRAUD_ALERT",
                    toAccountId,
                    fromAccountId,
                    0.0,
                    0.0,
                    "Fraud alert: more than 3 incoming transactions within 60 seconds"
            ));
        }

        return transaction;
    }

    public FraudDetector getFraudDetector() {
        return fraudDetector;
    }

    public Ledger getLedger() {
        return ledger;
    }
}package com.zedpay.service;

import com.zedpay.model.Account;
import com.zedpay.model.LedgerEntry;
import com.zedpay.model.MerchantAccount;
import com.zedpay.model.SavingsAccount;
import com.zedpay.repository.AccountRepository;
import com.zedpay.transaction.SendMoneyTransaction;
import com.zedpay.transaction.TopUpTransaction;
import com.zedpay.transaction.WithdrawTransaction;

public class TransactionService {
    private final AccountRepository accountRepository;
    private final FraudDetector fraudDetector;
    private final Ledger ledger;

    public TransactionService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
        this.fraudDetector = new FraudDetector();
        this.ledger = Ledger.getInstance();
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
        transaction.process();

        account.setBalance(account.getBalance() + amount);

        account.addTransaction(transaction.getSummary());

        accountRepository.update(account);

        ledger.recordEntry(new LedgerEntry(
                "TOPUP",
                accountId,
                null,
                amount,
                0.0,
                "Top up into account " + accountId
        ));

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
        double fee = transaction.calculateFee();
        double totalDebit = amount + fee;

        if (account instanceof SavingsAccount savingsAccount) {
            if ((account.getBalance() - totalDebit) < savingsAccount.getMinimumBalance()) {
                throw new IllegalArgumentException("Savings account cannot go below minimum balance");
            }
        } else {
            if (account.getBalance() < totalDebit) {
                throw new IllegalArgumentException("Insufficient balance");
            }
        }

        transaction.process();

        account.setBalance(account.getBalance() - totalDebit);

        account.addTransaction(transaction.getSummary());

        accountRepository.update(account);

        ledger.recordEntry(new LedgerEntry(
                "WITHDRAW",
                accountId,
                null,
                0.0,
                totalDebit,
                "Withdrawal from account " + accountId
        ));

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
        if (fromAccount == null) {
            throw new IllegalArgumentException("Sender account not found");
        }

        Account toAccount = accountRepository.findById(toAccountId);
        if (toAccount == null) {
            throw new IllegalArgumentException("Receiver account not found");
        }

        // Enforce merchant rule:
        // Merchant accounts can only receive transfers marked as merchant payments
        if (toAccount instanceof MerchantAccount && !merchantPayment) {
            throw new IllegalArgumentException("Merchant accounts can only receive merchant payments");
        }

        SendMoneyTransaction transaction = new SendMoneyTransaction(fromAccountId, toAccountId, amount);
        double fee = transaction.calculateFee();
        double totalDebit = amount + fee;

        if (fromAccount instanceof SavingsAccount savingsAccount) {
            if ((fromAccount.getBalance() - totalDebit) < savingsAccount.getMinimumBalance()) {
                throw new IllegalArgumentException("Savings account cannot go below minimum balance");
            }
        } else {
            if (fromAccount.getBalance() < totalDebit) {
                throw new IllegalArgumentException("Insufficient balance");
            }
        }

        boolean suspicious = fraudDetector.recordIncomingTransaction(toAccountId);

        transaction.process();

        fromAccount.setBalance(fromAccount.getBalance() - totalDebit);
        toAccount.setBalance(toAccount.getBalance() + amount);

        fromAccount.addTransaction(transaction.getSummary());
        toAccount.addTransaction(transaction.getSummary());

        accountRepository.update(fromAccount);
        accountRepository.update(toAccount);

        ledger.recordEntry(new LedgerEntry(
                merchantPayment ? "MERCHANT_PAYMENT" : "SEND_MONEY",
                fromAccountId,
                toAccountId,
                0.0,
                totalDebit,
                "Money sent from " + fromAccountId + " to " + toAccountId
        ));

        ledger.recordEntry(new LedgerEntry(
                merchantPayment ? "MERCHANT_PAYMENT" : "SEND_MONEY",
                toAccountId,
                fromAccountId,
                amount,
                0.0,
                "Money received by " + toAccountId + " from " + fromAccountId
        ));

        if (suspicious) {
            ledger.recordEntry(new LedgerEntry(
                    "FRAUD_ALERT",
                    toAccountId,
                    fromAccountId,
                    0.0,
                    0.0,
                    "Fraud alert: more than 3 incoming transactions within 60 seconds"
            ));
        }

        return transaction;
    }

    public FraudDetector getFraudDetector() {
        return fraudDetector;
    }

    public Ledger getLedger() {
        return ledger;
    }
}