package com.zedpay.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FraudDetector {
    private static final long WINDOW_SECONDS = 60;
    private static final int THRESHOLD = 3;

    private final Map<String, List<Instant>> incomingTransactionLog = new HashMap<>();

    public boolean recordIncomingTransaction(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            return false;
        }

        Instant now = Instant.now();

        List<Instant> timestamps = incomingTransactionLog.computeIfAbsent(accountId, key -> new ArrayList<>());

        timestamps.removeIf(timestamp -> timestamp.isBefore(now.minusSeconds(WINDOW_SECONDS)));
        timestamps.add(now);

        return timestamps.size() > THRESHOLD;
    }

    public int getRecentIncomingTransactionCount(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            return 0;
        }

        Instant now = Instant.now();

        List<Instant> timestamps = incomingTransactionLog.get(accountId);
        if (timestamps == null) {
            return 0;
        }

        timestamps.removeIf(timestamp -> timestamp.isBefore(now.minusSeconds(WINDOW_SECONDS)));
        return timestamps.size();
    }

    public void clearAccount(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            return;
        }

        incomingTransactionLog.remove(accountId);
    }

    public void clearAll() {
        incomingTransactionLog.clear();
    }
}