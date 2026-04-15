package com.zedpay.service;

import com.zedpay.model.StandardAccount;
import com.zedpay.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LedgerTest {

    private AccountRepository accountRepository;
    private TransactionService transactionService;
    private Ledger ledger;

    private String accountAId;
    private String accountBId;
    private String accountCId;

    @BeforeEach
    void setUp() {
        accountRepository = new AccountRepository();
        transactionService = new TransactionService(accountRepository);
        ledger = Ledger.getInstance();
        ledger.clear();

        StandardAccount accountA = new StandardAccount("ACC-A", "STD-1001", 10000.0, "USER-A");
        StandardAccount accountB = new StandardAccount("ACC-B", "STD-1002", 10000.0, "USER-B");
        StandardAccount accountC = new StandardAccount("ACC-C", "STD-1003", 10000.0, "USER-C");

        accountRepository.save(accountA);
        accountRepository.save(accountB);
        accountRepository.save(accountC);

        accountAId = accountA.getId();
        accountBId = accountB.getId();
        accountCId = accountC.getId();
    }

    @Test
    void ledgerShouldBalanceAfter100RandomSendMoneyTransactions() {
        Random random = new Random();

        String[] accountIds = {accountAId, accountBId, accountCId};

        for (int i = 0; i < 100; i++) {
            String fromAccountId = accountIds[random.nextInt(accountIds.length)];
            String toAccountId = accountIds[random.nextInt(accountIds.length)];

            while (fromAccountId.equals(toAccountId)) {
                toAccountId = accountIds[random.nextInt(accountIds.length)];
            }

            double amount = 1 + random.nextInt(200);

            transactionService.sendMoney(fromAccountId, toAccountId, amount, false);
        }

        assertEquals(200, ledger.getEntryCount());
        assertTrue(ledger.isBalanced());
        assertEquals(ledger.getTotalDebits(), ledger.getTotalCredits(), 0.0001);
    }
}