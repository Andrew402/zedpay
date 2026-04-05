package com.zedpay.transaction;

public interface Transaction {
    boolean process();
    double calculateFee();
    String getSummary();
}