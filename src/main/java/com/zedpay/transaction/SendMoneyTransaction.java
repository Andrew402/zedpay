package com.zedpay.transaction;

import com.zedpay.model.TransactionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class SendMoneyTransaction implements Transaction {
    private String transactionId;
    private String fromAccountId;
    private String toAccountId;
    private double amount;
    private double fee;
    private LocalDateTime timestamp;
    private TransactionStatus status;

    public SendMoneyTransaction(String fromAccountId, String toAccountId, double amount) {
        this.transactionId = UUID.randomUUID().toString();
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.fee = 0.0;
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
        this.fee = amount * 0.01;
        return fee;
    }

    @Override
    public String getSummary() {
        return "Send Money: " + amount + ", Status: " + status;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getFromAccountId() {
        return fromAccountId;
    }

    public String getToAccountId() {
        return toAccountId;
    }

    public double getAmount() {
        return amount;
    }

    public double getFee() {
        return fee;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }
}