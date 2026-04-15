package com.zedpay.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class LedgerEntry {
    private String entryId;
    private String transactionType;
    private String accountId;
    private String referenceAccountId;
    private double credit;
    private double debit;
    private String description;
    private LocalDateTime timestamp;

    public LedgerEntry() {
    }

    public LedgerEntry(String transactionType, String accountId, String referenceAccountId,
                       double credit, double debit, String description) {
        this.entryId = UUID.randomUUID().toString();
        this.transactionType = transactionType;
        this.accountId = accountId;
        this.referenceAccountId = referenceAccountId;
        this.credit = credit;
        this.debit = debit;
        this.description = description;
        this.timestamp = LocalDateTime.now();
    }

    public String getEntryId() {
        return entryId;
    }

    public void setEntryId(String entryId) {
        this.entryId = entryId;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getReferenceAccountId() {
        return referenceAccountId;
    }

    public void setReferenceAccountId(String referenceAccountId) {
        this.referenceAccountId = referenceAccountId;
    }

    public double getCredit() {
        return credit;
    }

    public void setCredit(double credit) {
        this.credit = credit;
    }

    public double getDebit() {
        return debit;
    }

    public void setDebit(double debit) {
        this.debit = debit;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}