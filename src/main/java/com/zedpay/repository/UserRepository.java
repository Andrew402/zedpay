package com.zedpay.repository;

import com.zedpay.database.DatabaseManager;
import com.zedpay.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UserRepository {

    public void save(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if (user.getId() == null || user.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("User ID is required");
        }

        String sql = """
                INSERT INTO users (id, full_name, phone_number, national_id, email, password)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    full_name = excluded.full_name,
                    phone_number = excluded.phone_number,
                    national_id = excluded.national_id,
                    email = excluded.email,
                    password = excluded.password
                """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getId());
            ps.setString(2, user.getFullName());
            ps.setString(3, user.getPhoneNumber());
            ps.setString(4, user.getNationalId());
            ps.setString(5, user.getEmail());
            ps.setString(6, user.getPassword());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save user: " + e.getMessage(), e);
        }
    }

    public User findById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT * FROM users WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user: " + e.getMessage(), e);
        }

        return null;
    }

    public User findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT * FROM users WHERE email = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user by email: " + e.getMessage(), e);
        }

        return null;
    }

    public User findByEmailAndPassword(String email, String password) {
        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT * FROM users WHERE email = ? AND password = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email.trim());
            ps.setString(2, password.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to authenticate user: " + e.getMessage(), e);
        }

        return null;
    }

    public boolean existsById(String id) {
        return findById(id) != null;
    }

    public boolean existsByEmail(String email) {
        return findByEmail(email) != null;
    }

    public Collection<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                users.add(mapUser(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load users: " + e.getMessage(), e);
        }

        return users;
    }

    private User mapUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getString("id"),
                rs.getString("full_name"),
                rs.getString("phone_number"),
                rs.getString("national_id"),
                rs.getString("email"),
                rs.getString("password")
        );
    }
}