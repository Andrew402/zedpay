package com.zedpay.service;

import com.zedpay.model.Account;
import com.zedpay.model.LedgerEntry;
import com.zedpay.model.MerchantAccount;
import com.zedpay.model.SavingsAccount;
import com.zedpay.repository.AccountRepository;
import com.zedpay.transaction.SendMoneyTransaction;
import com.zedpay.transaction.TopUpTransaction;
import com.zedpay.transaction.WithdrawTransaction;

import java.util.LinkedHashMap;
import java.util.Map;

public class TransactionService {
    private static final double CROSS_TYPE_SEND_FEE_RATE = 0.005; // 0.5%
    private static final String SYSTEM_FUNDING_ACCOUNT = "SYSTEM_FUNDING";
    private static final String SYSTEM_WITHDRAWAL_ACCOUNT = "SYSTEM_WITHDRAWAL";
    private static final String SYSTEM_FEE_REVENUE_ACCOUNT = "SYSTEM_FEE_REVENUE";

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
        double baseFee = transaction.calculateFee();
        double crossTypeFee = 0.0;
        double totalFee = baseFee + crossTypeFee;

        transaction.process();

        account.setBalance(account.getBalance() + amount);
        account.addTransaction(buildTopUpRecord(transaction, baseFee, crossTypeFee, totalFee));
        accountRepository.update(account);

        // Balanced ledger:
        // SYSTEM_FUNDING debit = user account credit
        ledger.recordEntry(new LedgerEntry(
                "TOPUP",
                SYSTEM_FUNDING_ACCOUNT,
                accountId,
                0.0,
                amount,
                "System funding source for top up into account " + accountId
        ));

        ledger.recordEntry(new LedgerEntry(
                "TOPUP",
                accountId,
                SYSTEM_FUNDING_ACCOUNT,
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
        double baseFee = transaction.calculateFee();
        double crossTypeFee = 0.0;
        double totalFee = baseFee + crossTypeFee;
        double totalDebit = amount + totalFee;

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
        account.addTransaction(buildWithdrawRecord(transaction, baseFee, crossTypeFee, totalFee));
        accountRepository.update(account);

        // Balanced ledger:
        // user debit = amount + fee
        // withdrawal outlet credit = amount
        // fee revenue credit = fee
        ledger.recordEntry(new LedgerEntry(
                "WITHDRAW",
                account.getId(),
                SYSTEM_WITHDRAWAL_ACCOUNT,
                0.0,
                totalDebit,
                "Withdrawal from account " + accountId
        ));

        ledger.recordEntry(new LedgerEntry(
                "WITHDRAW",
                SYSTEM_WITHDRAWAL_ACCOUNT,
                account.getId(),
                amount,
                0.0,
                "Cash/outflow paid for withdrawal from account " + accountId
        ));

        if (totalFee > 0) {
            ledger.recordEntry(new LedgerEntry(
                    "WITHDRAW_FEE",
                    SYSTEM_FEE_REVENUE_ACCOUNT,
                    account.getId(),
                    totalFee,
                    0.0,
                    "Fee revenue from withdrawal on account " + accountId
            ));
        }

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

        if (toAccount instanceof MerchantAccount && !merchantPayment) {
            throw new IllegalArgumentException("Merchant accounts can only receive merchant payments");
        }

        SendMoneyTransaction transaction = new SendMoneyTransaction(fromAccountId, toAccountId, amount);

        double baseFee = transaction.calculateFee();
        boolean crossType = isCrossTypeTransfer(fromAccount, toAccount);
        double crossTypeFee = crossType ? amount * CROSS_TYPE_SEND_FEE_RATE : 0.0;
        double totalFee = baseFee + crossTypeFee;
        double totalDebit = amount + totalFee;

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

        Map<String, Object> senderRecord = buildSendMoneyRecord(
                transaction,
                merchantPayment,
                "DEBIT",
                baseFee,
                crossTypeFee,
                totalFee,
                crossType
        );

        Map<String, Object> receiverRecord = buildSendMoneyRecord(
                transaction,
                merchantPayment,
                "CREDIT",
                baseFee,
                crossTypeFee,
                totalFee,
                crossType
        );

        fromAccount.addTransaction(senderRecord);
        toAccount.addTransaction(receiverRecord);

        accountRepository.update(fromAccount);
        accountRepository.update(toAccount);

        // Balanced ledger:
        // sender debit = amount + total fee
        // receiver credit = amount
        // fee revenue credit = total fee
        ledger.recordEntry(new LedgerEntry(
                merchantPayment ? "MERCHANT_PAYMENT" : "SEND_MONEY",
                fromAccountId,
                toAccountId,
                0.0,
                totalDebit,
                crossType
                        ? "Money sent from " + fromAccountId + " to " + toAccountId + " with cross-type fee"
                        : "Money sent from " + fromAccountId + " to " + toAccountId
        ));

        ledger.recordEntry(new LedgerEntry(
                merchantPayment ? "MERCHANT_PAYMENT" : "SEND_MONEY",
                toAccountId,
                fromAccountId,
                amount,
                0.0,
                crossType
                        ? "Money received by " + toAccountId + " from " + fromAccountId + " with cross-type fee applied"
                        : "Money received by " + toAccountId + " from " + fromAccountId
        ));

        if (totalFee > 0) {
            ledger.recordEntry(new LedgerEntry(
                    crossType ? "CROSS_TYPE_FEE" : "TRANSFER_FEE",
                    SYSTEM_FEE_REVENUE_ACCOUNT,
                    fromAccountId,
                    totalFee,
                    0.0,
                    crossType
                            ? "Fee revenue from transfer including cross-type fee"
                            : "Fee revenue from transfer"
            ));
        }

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

    private boolean isCrossTypeTransfer(Account fromAccount, Account toAccount) {
        return fromAccount.getAccountType() != toAccount.getAccountType();
    }

    private Map<String, Object> buildTopUpRecord(
            TopUpTransaction transaction,
            double baseFee,
            double crossTypeFee,
            double totalFee
    ) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("transactionId", transaction.getTransactionId());
        record.put("type", "TOPUP");
        record.put("accountId", transaction.getAccountId());
        record.put("amount", transaction.getAmount());
        record.put("baseFee", baseFee);
        record.put("crossTypeFee", crossTypeFee);
        record.put("totalFee", totalFee);
        record.put("timestamp", transaction.getTimestamp().toString());
        record.put("status", transaction.getStatus().name());
        return record;
    }

    private Map<String, Object> buildWithdrawRecord(
            WithdrawTransaction transaction,
            double baseFee,
            double crossTypeFee,
            double totalFee
    ) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("transactionId", transaction.getTransactionId());
        record.put("type", "WITHDRAW");
        record.put("accountId", transaction.getAccountId());
        record.put("amount", transaction.getAmount());
        record.put("baseFee", baseFee);
        record.put("crossTypeFee", crossTypeFee);
        record.put("totalFee", totalFee);
        record.put("timestamp", transaction.getTimestamp().toString());
        record.put("status", transaction.getStatus().name());
        return record;
    }

    private Map<String, Object> buildSendMoneyRecord(
            SendMoneyTransaction transaction,
            boolean merchantPayment,
            String direction,
            double baseFee,
            double crossTypeFee,
            double totalFee,
            boolean crossType
    ) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("transactionId", transaction.getTransactionId());
        record.put("type", merchantPayment ? "MERCHANT_PAYMENT" : "SEND_MONEY");
        record.put("direction", direction);
        record.put("fromAccountId", transaction.getFromAccountId());
        record.put("toAccountId", transaction.getToAccountId());
        record.put("amount", transaction.getAmount());
        record.put("baseFee", baseFee);
        record.put("crossTypeFee", crossTypeFee);
        record.put("totalFee", totalFee);
        record.put("crossType", crossType);
        record.put("timestamp", transaction.getTimestamp().toString());
        record.put("status", transaction.getStatus().name());
        return record;
    }
}