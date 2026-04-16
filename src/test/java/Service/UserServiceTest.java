package com.zedpay.service;

import com.zedpay.database.DatabaseManager;
import com.zedpay.model.User;
import com.zedpay.repository.AccountRepository;
import com.zedpay.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {

    private UserService userService;
    private UserRepository userRepository;
    private AccountRepository accountRepository;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseManager.initializeDatabase();
        clearDatabase();

        userRepository = new UserRepository();
        accountRepository = new AccountRepository();
        userService = new UserService(userRepository, accountRepository);
    }

    @Test
    void registerUserShouldCreateUserSuccessfully() {
        User user = userService.registerUser(
                "Andrew Lukwesa",
                "0977000000",
                "NRC001",
                "andrew1@test.com",
                "pass123"
        );

        assertNotNull(user);
        assertNotNull(user.getId());
        assertEquals("Andrew Lukwesa", user.getFullName());
    }

    @Test
    void registerUserShouldCreateDefaultStandardAccount() {
        User user = userService.registerUser(
                "Andrew Lukwesa",
                "0977000001",
                "NRC002",
                "andrew2@test.com",
                "pass123"
        );

        User savedUser = userService.getUserById(user.getId());

        assertNotNull(savedUser);
        assertNotNull(savedUser.getAccounts());
        assertEquals(1, savedUser.getAccounts().size());
        assertEquals("STANDARD", savedUser.getAccounts().get(0).getAccountType().name());
    }

    @Test
    void registerUserShouldRejectDuplicateEmail() {
        userService.registerUser(
                "Andrew Lukwesa",
                "0977000002",
                "NRC003",
                "andrew3@test.com",
                "pass123"
        );

        assertThrows(IllegalArgumentException.class, () ->
                userService.registerUser(
                        "Another User",
                        "0977009999",
                        "NRC999",
                        "andrew3@test.com",
                        "pass999"
                )
        );
    }

    @Test
    void getUserByIdShouldReturnUserWithLinkedAccounts() {
        User user = userService.registerUser(
                "Andrew Lukwesa",
                "0977000003",
                "NRC004",
                "andrew4@test.com",
                "pass123"
        );

        User loadedUser = userService.getUserById(user.getId());

        assertNotNull(loadedUser);
        assertEquals(user.getId(), loadedUser.getId());
        assertNotNull(loadedUser.getAccounts());
        assertFalse(loadedUser.getAccounts().isEmpty());
    }

    @Test
    void loginUserShouldReturnUserForCorrectCredentials() {
        userService.registerUser(
                "Andrew Lukwesa",
                "0977000004",
                "NRC005",
                "andrew5@test.com",
                "pass123"
        );

        User loggedInUser = userService.loginUser("andrew5@test.com", "pass123");

        assertNotNull(loggedInUser);
        assertEquals("andrew5@test.com", loggedInUser.getEmail());
    }

    @Test
    void loginUserShouldReturnNullForWrongPassword() {
        userService.registerUser(
                "Andrew Lukwesa",
                "0977000005",
                "NRC006",
                "andrew6@test.com",
                "pass123"
        );

        User loggedInUser = userService.loginUser("andrew6@test.com", "wrongpass");

        assertNull(loggedInUser);
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