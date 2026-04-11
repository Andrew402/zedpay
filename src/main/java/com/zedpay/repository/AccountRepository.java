package com.zedpay.repository;

import com.zedpay.database.DatabaseManager;
import com.zedpay.model.Account;
import com.zedpay.model.MerchantAccount;
import com.zedpay.model.SavingsAccount;
import com.zedpay.model.StandardAccount;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AccountRepository {

    public void save(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null");
        }

        if (account.getId() == null || account.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("Account ID is required");
        }

        String sql = """
                INSERT INTO accounts (
                    id, account_number, balance, owner_id, account_type, minimum_balance, business_registration_id
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    account_number = excluded.account_number,
                    balance = excluded.balance,
                    owner_id = excluded.owner_id,
                    account_type = excluded.account_type,
                    minimum_balance = excluded.minimum_balance,
                    business_registration_id = excluded.business_registration_id
                """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, account.getId());
            ps.setString(2, account.getAccountNumber());
            ps.setDouble(3, account.getBalance());
            ps.setString(4, account.getOwnerId());
            ps.setString(5, account.getAccountType().name());

            if (account instanceof SavingsAccount savingsAccount) {
                ps.setDouble(6, savingsAccount.getMinimumBalance());
            } else {
                ps.setDouble(6, 0.0);
            }

            if (account instanceof MerchantAccount merchantAccount) {
                ps.setString(7, merchantAccount.getBusinessRegistrationId());
            } else {
                ps.setNull(7, Types.VARCHAR);
            }

            ps.executeUpdate();
            saveTransactionHistory(conn, account);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save account: " + e.getMessage(), e);
        }
    }

    public Account findById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT * FROM accounts WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Account account = mapAccount(rs);
                    loadTransactionHistory(conn, account);
                    return account;
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find account: " + e.getMessage(), e);
        }

        return null;
    }

    public boolean existsById(String id) {
        return findById(id) != null;
    }

    public Collection<Account> findAll() {
        List<Account> accounts = new ArrayList<>();
        String sql = "SELECT * FROM accounts";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Account account = mapAccount(rs);
                loadTransactionHistory(conn, account);
                accounts.add(account);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load accounts: " + e.getMessage(), e);
        }

        return accounts;
    }

    public Collection<Account> findByOwnerId(String ownerId) {
        List<Account> accounts = new ArrayList<>();

        if (ownerId == null || ownerId.trim().isEmpty()) {
            return accounts;
        }

        String sql = "SELECT * FROM accounts WHERE owner_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, ownerId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Account account = mapAccount(rs);
                    loadTransactionHistory(conn, account);
                    accounts.add(account);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load user accounts: " + e.getMessage(), e);
        }

        return accounts;
    }

    private Account mapAccount(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String accountNumber = rs.getString("account_number");
        double balance = rs.getDouble("balance");
        String ownerId = rs.getString("owner_id");
        String accountType = rs.getString("account_type");

        return switch (accountType) {
            case "SAVINGS" -> new SavingsAccount(
                    id,
                    accountNumber,
                    balance,
                    ownerId,
                    rs.getDouble("minimum_balance")
            );
            case "MERCHANT" -> new MerchantAccount(
                    id,
                    accountNumber,
                    balance,
                    ownerId,
                    rs.getString("business_registration_id")
            );
            default -> new StandardAccount(
                    id,
                    accountNumber,
                    balance,
                    ownerId
            );
        };
    }

    private void saveTransactionHistory(Connection conn, Account account) throws SQLException {
        String deleteSql = "DELETE FROM transaction_history WHERE account_id = ?";
        try (PreparedStatement deletePs = conn.prepareStatement(deleteSql)) {
            deletePs.setString(1, account.getId());
            deletePs.executeUpdate();
        }

        String insertSql = "INSERT INTO transaction_history (account_id, record) VALUES (?, ?)";
        try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
            for (String record : account.getTransactionHistory()) {
                insertPs.setString(1, account.getId());
                insertPs.setString(2, record);
                insertPs.addBatch();
            }
            insertPs.executeBatch();
        }
    }

    private void loadTransactionHistory(Connection conn, Account account) throws SQLException {
        String sql = "SELECT record FROM transaction_history WHERE account_id = ? ORDER BY history_id";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, account.getId());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    account.addTransactionHistory(rs.getString("record"));
                }
            }
        }
    }
}