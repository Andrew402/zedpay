package com.zedpay.api;

import com.google.gson.Gson;
import com.zedpay.model.Account;
import com.zedpay.model.User;
import com.zedpay.repository.AccountRepository;
import com.zedpay.repository.UserRepository;
import com.zedpay.service.AccountService;
import com.zedpay.service.TransactionService;
import com.zedpay.service.UserService;
import io.javalin.Javalin;

import java.util.Map;

public class ZedPayApp {
    public static void main(String[] args) {
        UserRepository userRepository = new UserRepository();
        AccountRepository accountRepository = new AccountRepository();

        UserService userService = new UserService(userRepository, accountRepository);
        AccountService accountService = new AccountService(accountRepository, userRepository);
        TransactionService transactionService = new TransactionService(accountRepository);

        Gson gson = new Gson();

        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> it.anyHost());
            });
        }).start(7070);

        app.get("/", ctx -> ctx.result("ZedPay API is running"));

        app.post("/users/register", ctx -> {
            Map<String, Object> body = gson.fromJson(ctx.body(), Map.class);

            String fullName = (String) body.get("fullName");
            String phoneNumber = (String) body.get("phoneNumber");
            String nationalId = (String) body.get("nationalId");

            User user = userService.registerUser(fullName, phoneNumber, nationalId);
            ctx.status(201).json(user);
        });

        app.get("/users/{id}", ctx -> {
            String id = ctx.pathParam("id");
            User user = userService.getUserById(id);

            if (user == null) {
                ctx.status(404).json(Map.of("message", "User not found"));
                return;
            }

            ctx.json(user);
        });

        app.post("/accounts/create", ctx -> {
            Map<String, Object> body = gson.fromJson(ctx.body(), Map.class);

            String userId = (String) body.get("userId");
            String accountType = (String) body.get("accountType");
            String businessRegistrationId = body.get("businessRegistrationId") == null ? null : (String) body.get("businessRegistrationId");

            double minimumBalance = 0.0;
            if (body.get("minimumBalance") != null) {
                minimumBalance = ((Number) body.get("minimumBalance")).doubleValue();
            }

            Account account = accountService.createAccount(userId, accountType, businessRegistrationId, minimumBalance);

            if (account == null) {
                ctx.status(404).json(Map.of("message", "User not found"));
                return;
            }

            ctx.status(201).json(account);
        });

        app.post("/transactions/topup", ctx -> {
            Map<String, Object> body = gson.fromJson(ctx.body(), Map.class);

            String accountId = (String) body.get("accountId");
            double amount = ((Number) body.get("amount")).doubleValue();

            Object transaction = transactionService.topUp(accountId, amount);

            if (transaction == null) {
                ctx.status(400).json(Map.of("message", "Top up failed"));
                return;
            }

            ctx.status(201).json(transaction);
        });

        app.post("/transactions/withdraw", ctx -> {
            Map<String, Object> body = gson.fromJson(ctx.body(), Map.class);

            String accountId = (String) body.get("accountId");
            double amount = ((Number) body.get("amount")).doubleValue();

            Object transaction = transactionService.withdraw(accountId, amount);

            if (transaction == null) {
                ctx.status(400).json(Map.of("message", "Withdraw failed"));
                return;
            }

            ctx.status(201).json(transaction);
        });

        app.post("/transactions/send", ctx -> {
            Map<String, Object> body = gson.fromJson(ctx.body(), Map.class);

            String fromAccountId = (String) body.get("fromAccountId");
            String toAccountId = (String) body.get("toAccountId");
            double amount = ((Number) body.get("amount")).doubleValue();

            boolean merchantPayment = false;
            if (body.get("merchantPayment") != null) {
                merchantPayment = (Boolean) body.get("merchantPayment");
            }

            Object transaction = transactionService.sendMoney(fromAccountId, toAccountId, amount, merchantPayment);

            if (transaction == null) {
                ctx.status(400).json(Map.of("message", "Send money failed"));
                return;
            }

            ctx.status(201).json(transaction);
        });

        app.get("/accounts/{id}/statement", ctx -> {
            String accountId = ctx.pathParam("id");
            String statement = accountService.getStatement(accountId);

            if (statement == null) {
                ctx.status(404).json(Map.of("message", "Account not found"));
                return;
            }

            ctx.json(Map.of("statement", statement));
        });

        app.get("/accounts/{id}/history", ctx -> {
            String accountId = ctx.pathParam("id");
            Account account = accountService.getAccountById(accountId);

            if (account == null) {
                ctx.status(404).json(Map.of("message", "Account not found"));
                return;
            }

            ctx.json(account.getTransactionHistory());
        });
    }
}