package com.zedpay.service;

import com.zedpay.database.DatabaseManager;
import com.zedpay.model.Account;
import com.zedpay.model.MerchantAccount;
import com.zedpay.model.SavingsAccount;
import com.zedpay.model.StandardAccount;
import com.zedpay.model.User;
import com.zedpay.repository.AccountRepository;
import com.zedpay.repository.UserRepository;
import com.zedpay.transaction.SendMoneyTransaction;
import com.zedpay.transaction.TopUpTransaction;
import com.zedpay.transaction.WithdrawTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class TransactionServiceTest {

    private TransactionService transactionService;
    private AccountRepository accountRepository;
    private UserRepository userRepository;

    private StandardAccount sender;
    private StandardAccount receiver;
    private MerchantAccount merchant;
    private SavingsAccount savings;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseManager.initializeDatabase();
        clearDatabase();

        accountRepository = new AccountRepository();
        userRepository = new UserRepository();
        transactionService = new TransactionService(accountRepository);
        transactionService.getLedger().clear();

        User user1 = new User("U1", "Andrew", "0977000001", "NRC1", "u1@test.com", "pass1");
        User user2 = new User("U2", "Brian", "0977000002", "NRC2", "u2@test.com", "pass2");
        User user3 = new User("U3", "Merchant Owner", "0977000003", "NRC3", "u3@test.com", "pass3");
        User user4 = new User("U4", "Saver", "0977000004", "NRC4", "u4@test.com", "pass4");

        userRepository.save(user1);
        userRepository.save(user2);
        userRepository.save(user3);
        userRepository.save(user4);

        sender = new StandardAccount("A1", "STD-001", 1000.0, "U1");
        receiver = new StandardAccount("A2", "STD-002", 500.0, "U2");
        merchant = new MerchantAccount("M1", "MER-001", 200.0, "U3", "BIZ-123");
        savings = new SavingsAccount("S1", "SAV-001", 500.0, "U4", 100.0);

        accountRepository.save(sender);
        accountRepository.save(receiver);
        accountRepository.save(merchant);
        accountRepository.save(savings);
    }

    @Test
    void topUpShouldIncreaseBalance() {
        TopUpTransaction tx = transactionService.topUp("A1", 200.0);
        Account updated = accountRepository.findById("A1");

        assertNotNull(tx);
        assertEquals(1200.0, updated.getBalance(), 0.0001);
    }

    @Test
    void withdrawShouldReduceBalanceIncludingFee() {
        WithdrawTransaction tx = transactionService.withdraw("A1", 200.0);
        Account updated = accountRepository.findById("A1");

        assertNotNull(tx);
        assertEquals(799.0, updated.getBalance(), 0.0001);
    }

    @Test
    void withdrawShouldFailWhenInsufficientBalance() {
        assertThrows(IllegalArgumentException.class, () ->
                transactionService.withdraw("A2", 1000.0)
        );
    }

    @Test
    void savingsWithdrawShouldRespectMinimumBalance() {
        assertThrows(IllegalArgumentException.class, () ->
                transactionService.withdraw("S1", 450.0)
        );
    }

    @Test
    void sendMoneyShouldTransferAmountAndChargeFee() {
        SendMoneyTransaction tx = transactionService.sendMoney("A1", "A2", 100.0, false);

        Account updatedSender = accountRepository.findById("A1");
        Account updatedReceiver = accountRepository.findById("A2");

        assertNotNull(tx);
        assertEquals(899.0, updatedSender.getBalance(), 0.0001);
        assertEquals(600.0, updatedReceiver.getBalance(), 0.0001);
    }

    @Test
    void sendMoneyToMerchantShouldFailWhenMerchantPaymentIsFalse() {
        assertThrows(IllegalArgumentException.class, () ->
                transactionService.sendMoney("A1", "M1", 100.0, false)
        );
    }

    @Test
    void sendMoneyToMerchantShouldWorkWhenMerchantPaymentIsTrue() {
        SendMoneyTransaction tx = transactionService.sendMoney("A1", "M1", 100.0, true);

        Account updatedSender = accountRepository.findById("A1");
        Account updatedMerchant = accountRepository.findById("M1");

        assertNotNull(tx);
        assertEquals(898.5, updatedSender.getBalance(), 0.0001);
        assertEquals(300.0, updatedMerchant.getBalance(), 0.0001);
    }

    @Test
    void crossTypeTransferShouldChargeExtraFee() {
        transactionService.sendMoney("A1", "S1", 100.0, false);
        Account updatedSender = accountRepository.findById("A1");

        // 100 amount + 1 base fee + 0.5 cross-type fee = 101.5 total debit
        assertEquals(898.5, updatedSender.getBalance(), 0.0001);
    }

    private void clearDatabase() throws Exception {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM transaction_history");
            stmt.executeUpdate("DELETE FROM accounts");
            stmt.executeUpdate("DELETE FROM users");
        }
    }
}