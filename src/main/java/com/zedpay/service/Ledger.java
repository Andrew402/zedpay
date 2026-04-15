package com.zedpay.service;

import com.zedpay.model.LedgerEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Ledger {
    private static Ledger instance;
    private final List<LedgerEntry> entries;

    private Ledger() {
        this.entries = new ArrayList<>();
    }

    public static synchronized Ledger getInstance() {
        if (instance == null) {
            instance = new Ledger();
        }
        return instance;
    }

    public synchronized void recordEntry(LedgerEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("Ledger entry cannot be null");
        }

        entries.add(entry);
    }

    public synchronized List<LedgerEntry> getAllEntries() {
        return Collections.unmodifiableList(entries);
    }

    public synchronized double getTotalCredits() {
        double total = 0.0;
        for (LedgerEntry entry : entries) {
            total += entry.getCredit();
        }
        return total;
    }

    public synchronized double getTotalDebits() {
        double total = 0.0;
        for (LedgerEntry entry : entries) {
            total += entry.getDebit();
        }
        return total;
    }

    public synchronized boolean isBalanced() {
        return Math.abs(getTotalCredits() - getTotalDebits()) < 0.0001;
    }

    public synchronized int getEntryCount() {
        return entries.size();
    }

    public synchronized void clear() {
        entries.clear();
    }
}