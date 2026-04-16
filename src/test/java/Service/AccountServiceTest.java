package com.zedpay.service;

import com.zedpay.database.DatabaseManager;
import com.zedpay.model.Account;
import com.zedpay.model.MerchantAccount;
import com.zedpay.model.SavingsAccount;
import com.zedpay.model.StandardAccount;
import com.zedpay.model.User;
import com.zedpay.repository.AccountRepository;
import com.zedpay.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class AccountServiceTest {

    private AccountService accountService;
    private UserRepository userRepository;
    private AccountRepository accountRepository;
    private String userId;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseManager.initializeDatabase();
        clearDatabase();

        userRepository = new UserRepository();
        accountRepository = new AccountRepository();
        accountService = new AccountService(accountRepository, userRepository);

        User user = new User("USER-100", "Andrew", "0977111111", "NRC100", "acc@test.com", "pass123");
        userRepository.save(user);
        userId = user.getId();
    }

    @Test
    void createStandardAccountShouldSucceed() {
        Account account = accountService.createAccount(userId, "STANDARD", null, 0.0);

        assertNotNull(account);
        assertTrue(account instanceof StandardAccount);
    }

    @Test
    void createSavingsAccountShouldSucceed() {
        Account account = accountService.createAccount(userId, "SAVINGS", null, 100.0);

        assertNotNull(account);
        assertTrue(account instanceof SavingsAccount);
        assertEquals(100.0, ((SavingsAccount) account).getMinimumBalance());
    }

    @Test
    void createMerchantAccountShouldRequireBusinessRegistrationId() {
        assertThrows(IllegalArgumentException.class, () ->
                accountService.createAccount(userId, "MERCHANT", null, 0.0)
        );
    }

    @Test
    void createMerchantAccountShouldSucceedWithBusinessRegistrationId() {
        Account account = accountService.createAccount(userId, "MERCHANT", "BIZ-001", 0.0);

        assertNotNull(account);
        assertTrue(account instanceof MerchantAccount);
        assertEquals("BIZ-001", ((MerchantAccount) account).getBusinessRegistrationId());
    }

    @Test
    void getAccountByIdShouldReturnSavedAccount() {
        Account created = accountService.createAccount(userId, "STANDARD", null, 0.0);
        Account loaded = accountService.getAccountById(created.getId());

        assertNotNull(loaded);
        assertEquals(created.getId(), loaded.getId());
    }

    @Test
    void getStatementShouldReturnGeneratedStatement() {
        Account created = accountService.createAccount(userId, "STANDARD", null, 0.0);
        String statement = accountService.getStatement(created.getId());

        assertNotNull(statement);
        assertTrue(statement.contains("Statement"));
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