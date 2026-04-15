package com.zedpay.api;

import com.google.gson.Gson;
import com.zedpay.database.DatabaseManager;
import com.zedpay.model.Account;
import com.zedpay.model.User;
import com.zedpay.repository.AccountRepository;
import com.zedpay.repository.UserRepository;
import com.zedpay.service.AccountService;
import com.zedpay.service.Ledger;
import com.zedpay.service.TransactionService;
import com.zedpay.service.UserService;
import io.javalin.Javalin;

import java.util.Map;

public class ZedPayApp {
    public static void main(String[] args) {
        DatabaseManager.initializeDatabase();

        UserRepository userRepository = new UserRepository();
        AccountRepository accountRepository = new AccountRepository();

        UserService userService = new UserService(userRepository, accountRepository);
        AccountService accountService = new AccountService(accountRepository, userRepository);
        TransactionService transactionService = new TransactionService(accountRepository);

        Gson gson = new Gson();
        Ledger ledger = Ledger.getInstance();

        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> it.anyHost());
            });
        }).start(7070);

        app.get("/", ctx -> ctx.result("ZedPay API is running"));

        app.get("/users", ctx -> {
            try {
                ctx.json(userRepository.findAll());
            } catch (Exception e) {
                ctx.status(500).json(Map.of("message", "Failed to fetch users"));
            }
        });

        app.get("/accounts", ctx -> {
            try {
                ctx.json(accountRepository.findAll());
            } catch (Exception e) {
                ctx.status(500).json(Map.of("message", "Failed to fetch accounts"));
            }
        });

        app.post("/users/register", ctx -> {
            try {
                Map<String, Object> body = gson.fromJson(ctx.body(), Map.class);

                if (body == null) {
                    ctx.status(400).json(Map.of("message", "Request body is required"));
                    return;
                }

                String fullName = body.get("fullName") == null ? null : body.get("fullName").toString().trim();
                String phoneNumber = body.get("phoneNumber") == null ? null : body.get("phoneNumber").toString().trim();
                String nationalId = body.get("nationalId") == null ? null : body.get("nationalId").toString().trim();
                String email = body.get("email") == null ? null : body.get("email").toString().trim();
                String password = body.get("password") == null ? null : body.get("password").toString().trim();

                if (fullName == null || fullName.isEmpty()) {
                    ctx.status(400).json(Map.of("message", "Full name is required"));
                    return;
                }

                if (phoneNumber == null || phoneNumber.isEmpty()) {
                    ctx.status(400).json(Map.of("message", "Phone number is required"));
                    return;
                }

                if (nationalId == null || nationalId.isEmpty()) {
                    ctx.status(400).json(Map.of("message", "National ID is required"));
                    return;
                }

                if (email == null || email.isEmpty()) {
                    ctx.status(400).json(Map.of("message", "Email is required"));
                    return;
                }

                if (password == null || password.isEmpty()) {
                    ctx.status(400).json(Map.of("message", "Password is required"));
                    return;
                }

                User user = userService.registerUser(fullName, phoneNumber, nationalId, email, password);

                ctx.status(201).json(Map.of(
                        "message", "User registered successfully",
                        "user", user
                ));

            } catch (IllegalArgumentException e) {
                ctx.status(400).json(Map.of("message", e.getMessage()));
            } catch (Exception e) {
                ctx.status(500).json(Map.of("message", "Failed to register user"));
            }
        });

        app.post("/auth/login", ctx -> {
            try {
                Map<String, Object> body = gson.fromJson(ctx.body(), Map.class);

                if (body == null) {
                    ctx.status(400).json(Map.of("message", "Request body is required"));
                    return;
                }

                String email = body.get("email") == null ? null : body.get("email").toString().trim();
                String password = body.get("password") == null ? null : body.get("password").toString().trim();

                if (email == null || email.isEmpty()) {
                    ctx.status(400).json(Map.of("message", "Email is required"));
                    return;
                }

                if (password == null || password.isEmpty()) {
                    ctx.status(400).json(Map.of("message", "Password is required"));
                    return;
                }

                User user = userService.loginUser(email, password);

                if (user == null) {
                    ctx.status(401).json(Map.of("message", "Invalid email or password"));
                    return;
                }

                ctx.status(200).json(Map.of(
                        "message", "Login successful",
                        "user", user
                ));

            } catch (IllegalArgumentException e) {
                ctx.status(400).json(Map.of("message", e.getMessage()));
            } catch (Exception e) {
                ctx.status(500).json(Map.of("message", "Failed to login"));
            }
        });

        app.post("/users/login", ctx -> {
            try {
                Map<String, Object> body = gson.fromJson(ctx.body(), Map.class);

                if (body == null) {
                    ctx.status(400).json(Map.of("message", "Request body is required"));
                    return;
                }

                String email = body.get("email") == null ? null : body.get("email").toString().trim();
                String password = body.get("password") == null ? null : body.get("password").toString().trim();

                if (email == null || email.isEmpty()) {
                    ctx.status(400).json(Map.of("message", "Email is required"));
                    return;
                }

                if (password == null || password.isEmpty()) {
                    ctx.status(400).json(Map.of("message", "Password is required"));
                    return;
                }

                User user = userService.loginUser(email, password);

                if (user == null) {
                    ctx.status(401).json(Map.of("message", "Invalid email or password"));
                    return;
                }

                ctx.status(200).json(Map.of(
                        "message", "Login successful",
                        "user", user
                ));

            } catch (IllegalArgumentException e) {
                ctx.status(400).json(Map.of("message", e.getMessage()));
            } catch (Exception e) {
                ctx.status(500).json(Map.of("message", "Failed to login"));
            }
        });

        app.get("/users/{id}", ctx -> {
            try {
                String id = ctx.pathParam("id");

                if (id == null || id.trim().isEmpty()) {
                    ctx.status(400).json(Map.of("message", "User ID is required"));
                    return;
                }

                User user = userService.getUserById(id);

                if (user == null) {
                    ctx.status(404).json(Map.of("message", "User not found"));
                    return;
                }

                ctx.json(user);

            } catch (IllegalArgumentException e) {
                ctx.status(400).json(Map.of("message", e.getMessage()));
            } catch (Exception e) {
                ctx.status(500).json(Map.of("message", "Failed to fetch user"));
            }
        });

        app.get("/users/{id}/accounts", ctx -> {
            try {
                String id = ctx.pathParam("id");

                if (id == null || id.trim().isEmpty()) {
                    ctx.status(400).json(Map.of("message", "User ID is required"));
                    return;
                }

                User user = userService.getUserById(id);

                if (user == null) {
                    ctx.status(404).json(Map.of("message", "User not found"));
                    return;
                }

                ctx.json(accountRepository.findByOwnerId(id));

            } catch (IllegalArgumentException e) {
                ctx.status(400).json(Map.of("message", e.getMessage()));
            } catch (Exception e) {
                ctx.status(500).json(Map.of("message", "Failed to fetch user accounts"));
            }
        });

        app.post("/accounts/create", ctx -> {
            try {
                Map<String, Object> body = gson.fromJson(ctx.body(), Map.class);

                if (body == null) {
                    ctx.status(400).json(Map.of("message", "Request body is required"));
                    return;
                }

                String userId = body.get("userId") == null ? null : body.get("userId").toString().trim();
                String accountType = body.get("accountType") == null ? null : body.get("accountType").toString().trim();
                String businessRegistrationId = body.get("businessRegistrationId") == null
                        ? null : body.get("businessRegistrationId").toString().trim();

                double minimumBalance = 0.0;
                if (body.get("minimumBalance") != null) {
                    minimumBalance = ((Number) body.get("minimumBalance")).doubleValue();
                }

                if (userId == null || userId.isEmpty()) {
                    ctx.status(400).json(Map.of("message", "User ID is required"));
                    return;
                }

                if (accountType == null || accountType.isEmpty()) {
                    ctx.status(400).json(Map.of("message", "Account type is required"));
                    return;
                }

                if (minimumBalance < 0) {
                    ctx.status(400).json(Map.of("message", "Minimum balance cannot be negative"));
                    return;
                }

                Account account = accountService.createAccount(userId, accountType, businessRegistrationId, minimumBalance);

                if (account == null) {
                    ctx.status(404).json(Map.of("message", "User not found or account creation failed"));
                    return;
                }

                ctx.status(201).json(account);

            } catch (IllegalArgumentException e) {
                ctx.status(400).json(Map.of("message", e.getMessage()));
            } catch (Exception e) {
                ctx.status(500).json(Map.of("message", "Failed to create account"));
            }
        });

        app.post("/transactions/topup", ctx -> {
            try {
                Map<String, Object> body = gson.fromJson(ctx.body(), Map.class);

                if (body == null) {
                    ctx.status(400).json(Map.of("message", "Request body is required"));
                    return;
                }

                String accountId = body.get("accountId") == null ? null : body.get("accountId").toString().trim();

                if (accountId == null || accountId.isEmpty()) {
                    ctx.status(400).json(Map.of("message", "Account ID is required"));
                    return;
                }

                if (body.get("amount") == null) {
                    ctx.status(400).json(Map.of("message", "Amount is required"));
                    return;
                }

                double amount = ((Number) body.get("amount")).doubleValue();

                if (amount <= 0) {
                    ctx.status(400).json(Map.of("message", "Amount must be greater than zero"));
                    return;
                }

                Object transaction = transactionService.topUp(accountId, amount);
                ctx.status(201).json(transaction);

            } catch (IllegalArgumentException e) {
                ctx.status(400).json(Map.of("message", e.getMessage()));
            } catch (Exception e) {
                ctx.status(500).json(Map.of("message", "Failed to process top up"));
            }
        });

        app.post("/transactions/withdraw", ctx -> {
            try {
                Map<String, Object> body = gson.fromJson(ctx.body(), Map.class);

                if (body == null) {
                    ctx.status(400).json(Map.of("message", "Request body is required"));
                    return;
                }

                String accountId = body.get("accountId") == null ? null : body.get("accountId").toString().trim();

                if (accountId == null || accountId.isEmpty()) {
                    ctx.status(400).json(Map.of("message", "Account ID is required"));
                    return;
                }

                if (body.get("amount") == null) {
                    ctx.status(400).json(Map.of("message", "Amount is required"));
                    return;
                }

                double amount = ((Number) body.get("amount")).doubleValue();

                if (amount <= 0) {
                    ctx.status(400).json(Map.of("message", "Amount must be greater than zero"));
                    return;
                }

                Object transaction = transactionService.withdraw(accountId, amount);
                ctx.status(201).json(transaction);

            } catch (IllegalArgumentException e) {
                ctx.status(400).json(Map.of("message", e.getMessage()));
            } catch (Exception e) {
                ctx.status(500).json(Map.of("message", "Failed to process withdrawal"));
            }
        });

        app.post("/transactions/send", ctx -> {
            try {
                Map<String, Object> body = gson.fromJson(ctx.body(), Map.class);

                if (body == null) {
                    ctx.status(400).json(Map.of("message", "Request body is required"));
                    return;
                }

                String fromAccountId = body.get("fromAccountId") == null ? null : body.get("fromAccountId").toString().trim();
                String toAccountId = body.get("toAccountId") == null ? null : body.get("toAccountId").toString().trim();

                if (fromAccountId == null || fromAccountId.isEmpty()) {
                    ctx.status(400).json(Map.of("message", "Sender account ID is required"));
                    return;
                }

                if (toAccountId == null || toAccountId.isEmpty()) {
                    ctx.status(400).json(Map.of("message", "Receiver account ID is required"));
                    return;
                }

                if (fromAccountId.equals(toAccountId)) {
                    ctx.status(400).json(Map.of("message", "Cannot send money to the same account"));
                    return;
                }

                if (body.get("amount") == null) {
                    ctx.status(400).json(Map.of("message", "Amount is required"));
                    return;
                }

                double amount = ((Number) body.get("amount")).doubleValue();

                if (amount <= 0) {
                    ctx.status(400).json(Map.of("message", "Amount must be greater than zero"));
                    return;
                }

                boolean merchantPayment = false;
                if (body.get("merchantPayment") != null) {
                    merchantPayment = (Boolean) body.get("merchantPayment");
                }

                Object transaction = transactionService.sendMoney(fromAccountId, toAccountId, amount, merchantPayment);
                ctx.status(201).json(transaction);

            } catch (IllegalArgumentException e) {
                ctx.status(400).json(Map.of("message", e.getMessage()));
            } catch (Exception e) {
                ctx.status(500).json(Map.of("message", "Failed to process send money"));
            }
        });

        app.get("/accounts/{id}/statement", ctx -> {
            try {
                String accountId = ctx.pathParam("id");

                if (accountId == null || accountId.trim().isEmpty()) {
                    ctx.status(400).json(Map.of("message", "Account ID is required"));
                    return;
                }

                String statement = accountService.getStatement(accountId);

                if (statement == null) {
                    ctx.status(404).json(Map.of("message", "Account not found"));
                    return;
                }

                ctx.json(Map.of("statement", statement));

            } catch (IllegalArgumentException e) {
                ctx.status(400).json(Map.of("message", e.getMessage()));
            } catch (Exception e) {
                ctx.status(500).json(Map.of("message", "Failed to fetch statement"));
            }
        });

        app.get("/accounts/{id}/history", ctx -> {
            try {
                String accountId = ctx.pathParam("id");

                if (accountId == null || accountId.trim().isEmpty()) {
                    ctx.status(400).json(Map.of("message", "Account ID is required"));
                    return;
                }

                Account account = accountService.getAccountById(accountId);

                if (account == null) {
                    ctx.status(404).json(Map.of("message", "Account not found"));
                    return;
                }

                ctx.json(account.getTransactionHistory());

            } catch (IllegalArgumentException e) {
                ctx.status(400).json(Map.of("message", e.getMessage()));
            } catch (Exception e) {
                ctx.status(500).json(Map.of("message", "Failed to fetch transaction history"));
            }
        });

        app.get("/ledger", ctx -> {
            try {
                ctx.json(ledger.getAllEntries());
            } catch (Exception e) {
                ctx.status(500).json(Map.of("message", "Failed to fetch ledger entries"));
            }
        });

        app.get("/ledger/summary", ctx -> {
            try {
                ctx.json(Map.of(
                        "entryCount", ledger.getEntryCount(),
                        "totalCredits", ledger.getTotalCredits(),
                        "totalDebits", ledger.getTotalDebits(),
                        "balanced", ledger.isBalanced()
                ));
            } catch (Exception e) {
                ctx.status(500).json(Map.of("message", "Failed to fetch ledger summary"));
            }
        });

        app.post("/ledger/clear", ctx -> {
            try {
                ledger.clear();
                ctx.json(Map.of("message", "Ledger cleared successfully"));
            } catch (Exception e) {
                ctx.status(500).json(Map.of("message", "Failed to clear ledger"));
            }
        });
    }
}