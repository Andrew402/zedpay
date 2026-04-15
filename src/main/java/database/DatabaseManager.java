package com.zedpay.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:zedpay.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static void initializeDatabase() {
        createUsersTable();
        createAccountsTable();
        createTransactionHistoryTable();
        ensureUserColumns();
    }

    private static void createUsersTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS users (
                    id TEXT PRIMARY KEY,
                    full_name TEXT NOT NULL,
                    phone_number TEXT NOT NULL,
                    national_id TEXT NOT NULL,
                    email TEXT,
                    password TEXT
                )
                """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create users table: " + e.getMessage(), e);
        }
    }

    private static void createAccountsTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS accounts (
                    id TEXT PRIMARY KEY,
                    account_number TEXT NOT NULL,
                    balance REAL NOT NULL,
                    owner_id TEXT NOT NULL,
                    account_type TEXT NOT NULL,
                    minimum_balance REAL DEFAULT 0,
                    business_registration_id TEXT,
                    FOREIGN KEY (owner_id) REFERENCES users(id)
                )
                """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create accounts table: " + e.getMessage(), e);
        }
    }

    private static void createTransactionHistoryTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS transaction_history (
                    history_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    account_id TEXT NOT NULL,
                    record TEXT NOT NULL,
                    FOREIGN KEY (account_id) REFERENCES accounts(id)
                )
                """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create transaction history table: " + e.getMessage(), e);
        }
    }

    private static void ensureUserColumns() {
        ensureColumnExists("users", "email", "TEXT");
        ensureColumnExists("users", "password", "TEXT");
    }

    private static void ensureColumnExists(String tableName, String columnName, String columnDefinition) {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            boolean exists = false;
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + tableName + ")");

            while (rs.next()) {
                String existingColumn = rs.getString("name");
                if (columnName.equalsIgnoreCase(existingColumn)) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                stmt.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to ensure column " + columnName + ": " + e.getMessage(), e);
        }
    }
}