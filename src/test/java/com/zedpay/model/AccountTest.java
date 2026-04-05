package com.zedpay.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AccountTest {

    @Test
    void standardAccountShouldDepositMoney() {
        StandardAccount account = new StandardAccount("1", "STD001", 100.0, "U1");
        account.deposit(50.0);
        assertEquals(150.0, account.getBalance());
    }

    @Test
    void standardAccountShouldWithdrawIfEnoughBalance() {
        StandardAccount account = new StandardAccount("1", "STD001", 100.0, "U1");
        assertTrue(account.withdraw(40.0));
        assertEquals(60.0, account.getBalance());
    }

    @Test
    void standardAccountShouldNotWithdrawIfInsufficientBalance() {
        StandardAccount account = new StandardAccount("1", "STD001", 100.0, "U1");
        assertFalse(account.withdraw(150.0));
        assertEquals(100.0, account.getBalance());
    }

    @Test
    void savingsAccountShouldRespectMinimumBalance() {
        SavingsAccount account = new SavingsAccount("2", "SAV001", 500.0, "U2", 200.0);
        assertFalse(account.withdraw(350.0));
        assertEquals(500.0, account.getBalance());
    }

    @Test
    void savingsAccountShouldAllowValidWithdrawal() {
        SavingsAccount account = new SavingsAccount("2", "SAV001", 500.0, "U2", 200.0);
        assertTrue(account.withdraw(200.0));
        assertEquals(300.0, account.getBalance());
    }
}