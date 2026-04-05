package com.zedpay.transaction;

import com.zedpay.model.TransactionStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TransactionTest {

    @Test
    void sendMoneyTransactionShouldStartAsPending() {
        SendMoneyTransaction tx = new SendMoneyTransaction("A1", "A2", 100.0);
        assertEquals(TransactionStatus.PENDING, tx.getStatus());
    }

    @Test
    void sendMoneyTransactionShouldHaveTransactionId() {
        SendMoneyTransaction tx = new SendMoneyTransaction("A1", "A2", 100.0);
        assertNotNull(tx.getTransactionId());
    }

    @Test
    void sendMoneyTransactionShouldProcessSuccessfully() {
        SendMoneyTransaction tx = new SendMoneyTransaction("A1", "A2", 100.0);
        assertTrue(tx.process());
        assertEquals(TransactionStatus.SUCCESS, tx.getStatus());
    }

    @Test
    void topUpTransactionShouldHaveZeroFee() {
        TopUpTransaction tx = new TopUpTransaction("A1", 200.0);
        assertEquals(0.0, tx.calculateFee());
    }

    @Test
    void withdrawTransactionShouldCalculateFee() {
        WithdrawTransaction tx = new WithdrawTransaction("A1", 200.0);
        assertEquals(1.0, tx.calculateFee());
    }
}