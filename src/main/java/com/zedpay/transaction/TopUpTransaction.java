package com.zedpay.transaction;

import com.zedpay.model.TransactionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class TopUpTransaction implements Transaction {
    private String transactionId;
    private String accountId;
    private double amount;
    private double fee;
    private LocalDateTime timestamp;
    private TransactionStatus status;

    public TopUpTransaction(String accountId, double amount) {
        this.transactionId = UUID.randomUUID().toString();
        this.accountId = accountId;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
        this.status = TransactionStatus.PENDING;
    }

    @Override
    public boolean process() {
        this.status = TransactionStatus.SUCCESS;
        return true;
    }

    @Override
    public double calculateFee() {
        this.fee = 0.0;
        return fee;
    }

    @Override
    public String getSummary() {
        return "Top Up: " + amount + ", Status: " + status;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public TransactionStatus getStatus() {
        return status;
    }
}