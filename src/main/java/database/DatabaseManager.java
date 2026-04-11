package com.zedpay.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String URL = "jdbc:sqlite:zedpay.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void initializeDatabase() {
        String createUsersTable = """
                CREATE TABLE IF NOT EXISTS users (
                    id TEXT PRIMARY KEY,
                    full_name TEXT NOT NULL,
                    phone_number TEXT NOT NULL,
                    national_id TEXT NOT NULL
                );
                """;

        String createAccountsTable = """
                CREATE TABLE IF NOT EXISTS accounts (
                    id TEXT PRIMARY KEY,
                    account_number TEXT NOT NULL,
                    balance REAL NOT NULL,
                    owner_id TEXT NOT NULL,
                    account_type TEXT NOT NULL,
                    minimum_balance REAL DEFAULT 0,
                    business_registration_id TEXT,
                    FOREIGN KEY (owner_id) REFERENCES users(id)
                );
                """;

        String createTransactionHistoryTable = """
                CREATE TABLE IF NOT EXISTS transaction_history (
                    history_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    account_id TEXT NOT NULL,
                    record TEXT NOT NULL,
                    FOREIGN KEY (account_id) REFERENCES accounts(id)
                );
                """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createUsersTable);
            stmt.execute(createAccountsTable);
            stmt.execute(createTransactionHistoryTable);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database: " + e.getMessage(), e);
        }
    }
}