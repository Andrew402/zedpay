# ZedPay Digital Wallet System

## Group Assignment – Scenario A

ZedPay is a Java-based digital wallet backend system built for the DIT 400 Object-Oriented Programming group assignment.  
The system models users, accounts, and transactions using Object-Oriented Programming principles and exposes REST API endpoints using **Javalin**.

This project supports:
- user registration and login
- multiple account types
- money transfers
- top-ups
- withdrawals
- account statements
- transaction history
- fraud detection
- ledger tracking

---

# 1. Project Overview

ZedPay is a simplified digital wallet platform where users can create accounts and perform financial transactions.

The system supports three account types:

- **StandardAccount** – normal wallet account
- **SavingsAccount** – wallet with a minimum balance rule
- **MerchantAccount** – can only receive transfers marked as merchant payments

The system also supports three transaction types:

- **TopUpTransaction**
- **WithdrawTransaction**
- **SendMoneyTransaction**

The backend is built with:
- **Java**
- **Javalin**
- **SQLite**
- **JUnit 5**
- **Maven**

---

# 2. OOP Concepts Used

This project demonstrates key OOP principles:

## Encapsulation
Data is stored inside classes using private fields with public getters and setters.

Examples:
- `Account`
- `User`
- `LedgerEntry`

## Inheritance
Different account types inherit from the abstract `Account` class.

Examples:
- `StandardAccount extends Account`
- `SavingsAccount extends Account`
- `MerchantAccount extends Account`

## Abstraction
`Account` is an abstract class and `Transaction` is an interface.

Examples:
- `Account`
- `Transaction`

## Polymorphism
Different account types generate their own statements using the same method name.

Example:
- `generateStatement()`

---

# 3. Class Design

## Main Model Classes
- `User`
- `Account` (abstract)
- `StandardAccount`
- `SavingsAccount`
- `MerchantAccount`
- `LedgerEntry`

## Transaction Classes
- `Transaction` (interface)
- `TopUpTransaction`
- `WithdrawTransaction`
- `SendMoneyTransaction`

## Service Classes
- `UserService`
- `AccountService`
- `TransactionService`
- `FraudDetector`
- `Ledger`

## Repository Classes
- `UserRepository`
- `AccountRepository`

## API Layer
- `ZedPayApp`

---

# 4. Business Rules Implemented

The following business rules are enforced in the system:

1. A `SavingsAccount` cannot go below its minimum balance.
2. A `MerchantAccount` can only receive transfers when `merchantPayment = true`.
3. Cross-type transfers between different account types attract an extra fee.
4. Each transaction stores:
   - transaction ID
   - type
   - amount
   - fee
   - timestamp
   - status
5. Fraud detection flags repeated incoming transactions within a short time window.
6. The ledger records credits and debits and is tested for balancing.

---

# 5. API Endpoints

Base URL:
```http
http://localhost:7070