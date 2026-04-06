package com.zedpay.repository;

import com.zedpay.model.User;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class UserRepository {
    private final Map<String, User> users = new HashMap<>();

    public void save(User user) {
        users.put(user.getId(), user);
    }

    public User findById(String id) {
        return users.get(id);
    }

    public boolean existsById(String id) {
        return users.containsKey(id);
    }

    public Collection<User> findAll() {
        return users.values();
    }
}