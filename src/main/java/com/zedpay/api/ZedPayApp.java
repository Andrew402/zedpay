package com.zedpay.api;

import com.google.gson.Gson;
import com.zedpay.database.DatabaseManager;
import com.zedpay.exception.ApiError;
import com.zedpay.exception.ApiException;
import com.zedpay.exception.NotFoundException;
import com.zedpay.exception.ProcessingException;
import com.zedpay.exception.ValidationException;
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

    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        DatabaseManager.initializeDatabase();

        UserRepository userRepository = new UserRepository();
        AccountRepository accountRepository = new AccountRepository();

        UserService userService = new UserService(userRepository, accountRepository);
        AccountService accountService = new AccountService(accountRepository, userRepository);
        TransactionService transactionService = new TransactionService(accountRepository);

        Ledger ledger = Ledger.getInstance();

        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> it.anyHost());
            });
        });

        registerExceptionHandlers(app);
        registerRoutes(app, userRepository, accountRepository, userService, accountService, transactionService, ledger);

        app.start(7070);
    }

    private static void registerExceptionHandlers(Javalin app) {
        app.exception(ApiException.class, (e, ctx) -> {
            ctx.status(e.getStatusCode());
            ctx.json(new ApiError(e.getErrorCode(), e.getMessage()));
        });

        app.exception(IllegalArgumentException.class, (e, ctx) -> {
            ctx.status(400);
            ctx.json(new ApiError("VALIDATION_ERROR", e.getMessage()));
        });

        app.exception(Exception.class, (e, ctx) -> {
            ctx.status(500);
            ctx.json(new ApiError("PROCESSING_ERROR", "An unexpected error occurred"));
        });
    }

    private static void registerRoutes(
            Javalin app,
            UserRepository userRepository,
            AccountRepository accountRepository,
            UserService userService,
            AccountService accountService,
            TransactionService transactionService,
            Ledger ledger
    ) {
        app.get("/", ctx -> ctx.result("ZedPay API is running"));

        app.get("/users", ctx -> {
            try {
                ctx.json(userRepository.findAll());
            } catch (Exception e) {
                throw new ProcessingException("Failed to fetch users");
            }
        });

        app.get("/accounts", ctx -> {
            try {
                ctx.json(accountRepository.findAll());
            } catch (Exception e) {
                throw new ProcessingException("Failed to fetch accounts");
            }
        });

        app.get("/accounts/{id}", ctx -> {
            String accountId = ctx.pathParam("id");

            if (accountId == null || accountId.trim().isEmpty()) {
                throw new ValidationException("Account ID is required");
            }

            try {
                Account account = accountService.getAccountById(accountId);

                if (account == null) {
                    throw new NotFoundException("Account not found");
                }

                ctx.json(account);
            } catch (ApiException e) {
                throw e;
            } catch (IllegalArgumentException e) {
                throw new ValidationException(e.getMessage());
            } catch (Exception e) {
                throw new ProcessingException("Failed to fetch account");
            }
        });

        app.post("/users/register", ctx -> {
            Map<String, Object> body = parseBody(ctx.body());

            String fullName = requiredString(body, "fullName", "Full name is required");
            String phoneNumber = requiredString(body, "phoneNumber", "Phone number is required");
            String nationalId = requiredString(body, "nationalId", "National ID is required");
            String email = requiredString(body, "email", "Email is required");
            String password = requiredString(body, "password", "Password is required");

            try {
                User user = userService.registerUser(fullName, phoneNumber, nationalId, email, password);

                ctx.status(201).json(Map.of(
                        "message", "User registered successfully",
                        "user", user
                ));
            } catch (IllegalArgumentException e) {
                throw new ValidationException(e.getMessage());
            } catch (Exception e) {
                throw new ProcessingException("Failed to register user");
            }
        });

        app.post("/auth/login", ctx -> handleLogin(ctx, userService));
        app.post("/users/login", ctx -> handleLogin(ctx, userService));

        app.get("/users/{id}", ctx -> {
            String id = ctx.pathParam("id");

            if (id == null || id.trim().isEmpty()) {
                throw new ValidationException("User ID is required");
            }

            try {
                User user = userService.getUserById(id);

                if (user == null) {
                    throw new NotFoundException("User not found");
                }

                ctx.json(user);
            } catch (ApiException e) {
                throw e;
            } catch (IllegalArgumentException e) {
                throw new ValidationException(e.getMessage());
            } catch (Exception e) {
                throw new ProcessingException("Failed to fetch user");
            }
        });

        app.get("/users/{id}/accounts", ctx -> {
            String id = ctx.pathParam("id");

            if (id == null || id.trim().isEmpty()) {
                throw new ValidationException("User ID is required");
            }

            try {
                User user = userService.getUserById(id);

                if (user == null) {
                    throw new NotFoundException("User not found");
                }

                ctx.json(accountRepository.findByOwnerId(id));
            } catch (ApiException e) {
                throw e;
            } catch (IllegalArgumentException e) {
                throw new ValidationException(e.getMessage());
            } catch (Exception e) {
                throw new ProcessingException("Failed to fetch user accounts");
            }
        });

        app.post("/accounts/create", ctx -> {
            Map<String, Object> body = parseBody(ctx.body());

            String userId = requiredString(body, "userId", "User ID is required");
            String accountType = requiredString(body, "accountType", "Account type is required");
            String businessRegistrationId = optionalString(body, "businessRegistrationId");
            double minimumBalance = optionalDouble(body, "minimumBalance", 0.0);

            if (minimumBalance < 0) {
                throw new ValidationException("Minimum balance cannot be negative");
            }

            try {
                Account account = accountService.createAccount(userId, accountType, businessRegistrationId, minimumBalance);

                if (account == null) {
                    throw new NotFoundException("User not found or account creation failed");
                }

                ctx.status(201).json(account);
            } catch (ApiException e) {
                throw e;
            } catch (IllegalArgumentException e) {
                throw new ValidationException(e.getMessage());
            } catch (Exception e) {
                throw new ProcessingException("Failed to create account");
            }
        });

        app.post("/transactions/topup", ctx -> {
            Map<String, Object> body = parseBody(ctx.body());

            String accountId = requiredString(body, "accountId", "Account ID is required");
            double amount = requiredDouble(body, "amount", "Amount is required");

            if (amount <= 0) {
                throw new ValidationException("Amount must be greater than zero");
            }

            try {
                Object transaction = transactionService.topUp(accountId, amount);
                ctx.status(201).json(transaction);
            } catch (IllegalArgumentException e) {
                throw new ValidationException(e.getMessage());
            } catch (Exception e) {
                throw new ProcessingException("Failed to process top up");
            }
        });

        app.post("/transactions/withdraw", ctx -> {
            Map<String, Object> body = parseBody(ctx.body());

            String accountId = requiredString(body, "accountId", "Account ID is required");
            double amount = requiredDouble(body, "amount", "Amount is required");

            if (amount <= 0) {
                throw new ValidationException("Amount must be greater than zero");
            }

            try {
                Object transaction = transactionService.withdraw(accountId, amount);
                ctx.status(201).json(transaction);
            } catch (IllegalArgumentException e) {
                throw new ValidationException(e.getMessage());
            } catch (Exception e) {
                throw new ProcessingException("Failed to process withdrawal");
            }
        });

        app.post("/transactions/send", ctx -> {
            Map<String, Object> body = parseBody(ctx.body());

            String fromAccountId = requiredString(body, "fromAccountId", "Sender account ID is required");
            String toAccountId = requiredString(body, "toAccountId", "Receiver account ID is required");
            double amount = requiredDouble(body, "amount", "Amount is required");
            boolean merchantPayment = optionalBoolean(body, "merchantPayment", false);

            if (fromAccountId.equals(toAccountId)) {
                throw new ValidationException("Cannot send money to the same account");
            }

            if (amount <= 0) {
                throw new ValidationException("Amount must be greater than zero");
            }

            try {
                Object transaction = transactionService.sendMoney(fromAccountId, toAccountId, amount, merchantPayment);
                ctx.status(201).json(transaction);
            } catch (IllegalArgumentException e) {
                throw new ValidationException(e.getMessage());
            } catch (Exception e) {
                throw new ProcessingException("Failed to process send money");
            }
        });

        app.get("/accounts/{id}/statement", ctx -> {
            String accountId = ctx.pathParam("id");

            if (accountId == null || accountId.trim().isEmpty()) {
                throw new ValidationException("Account ID is required");
            }

            try {
                String statement = accountService.getStatement(accountId);

                if (statement == null) {
                    throw new NotFoundException("Account not found");
                }

                ctx.json(Map.of("statement", statement));
            } catch (ApiException e) {
                throw e;
            } catch (IllegalArgumentException e) {
                throw new ValidationException(e.getMessage());
            } catch (Exception e) {
                throw new ProcessingException("Failed to fetch statement");
            }
        });

        app.get("/accounts/{id}/history", ctx -> {
            String accountId = ctx.pathParam("id");

            if (accountId == null || accountId.trim().isEmpty()) {
                throw new ValidationException("Account ID is required");
            }

            try {
                Account account = accountService.getAccountById(accountId);

                if (account == null) {
                    throw new NotFoundException("Account not found");
                }

                ctx.json(account.getTransactionHistory());
            } catch (ApiException e) {
                throw e;
            } catch (IllegalArgumentException e) {
                throw new ValidationException(e.getMessage());
            } catch (Exception e) {
                throw new ProcessingException("Failed to fetch transaction history");
            }
        });

        app.get("/ledger", ctx -> {
            try {
                ctx.json(ledger.getAllEntries());
            } catch (Exception e) {
                throw new ProcessingException("Failed to fetch ledger entries");
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
                throw new ProcessingException("Failed to fetch ledger summary");
            }
        });

        app.post("/ledger/clear", ctx -> {
            try {
                ledger.clear();
                ctx.json(Map.of("message", "Ledger cleared successfully"));
            } catch (Exception e) {
                throw new ProcessingException("Failed to clear ledger");
            }
        });
    }

    private static void handleLogin(io.javalin.http.Context ctx, UserService userService) {
        Map<String, Object> body = parseBody(ctx.body());

        String email = requiredString(body, "email", "Email is required");
        String password = requiredString(body, "password", "Password is required");

        try {
            User user = userService.loginUser(email, password);

            if (user == null) {
                throw new ValidationException("Invalid email or password");
            }

            ctx.status(200).json(Map.of(
                    "message", "Login successful",
                    "user", user
            ));
        } catch (ApiException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new ValidationException(e.getMessage());
        } catch (Exception e) {
            throw new ProcessingException("Failed to login");
        }
    }

    private static Map<String, Object> parseBody(String requestBody) {
        Map<String, Object> body = gson.fromJson(requestBody, Map.class);

        if (body == null) {
            throw new ValidationException("Request body is required");
        }

        return body;
    }

    private static String requiredString(Map<String, Object> body, String key, String errorMessage) {
        Object value = body.get(key);

        if (value == null || value.toString().trim().isEmpty()) {
            throw new ValidationException(errorMessage);
        }

        return value.toString().trim();
    }

    private static String optionalString(Map<String, Object> body, String key) {
        Object value = body.get(key);
        return value == null ? null : value.toString().trim();
    }

    private static double requiredDouble(Map<String, Object> body, String key, String errorMessage) {
        Object value = body.get(key);

        if (value == null) {
            throw new ValidationException(errorMessage);
        }

        if (!(value instanceof Number)) {
            throw new ValidationException(key + " must be a number");
        }

        return ((Number) value).doubleValue();
    }

    private static double optionalDouble(Map<String, Object> body, String key, double defaultValue) {
        Object value = body.get(key);

        if (value == null) {
            return defaultValue;
        }

        if (!(value instanceof Number)) {
            throw new ValidationException(key + " must be a number");
        }

        return ((Number) value).doubleValue();
    }

    private static boolean optionalBoolean(Map<String, Object> body, String key, boolean defaultValue) {
        Object value = body.get(key);

        if (value == null) {
            return defaultValue;
        }

        if (value instanceof Boolean) {
            return (Boolean) value;
        }

        if (value instanceof String) {
            return Boolean.parseBoolean(value.toString());
        }

        throw new ValidationException(key + " must be true or false");
    }
}