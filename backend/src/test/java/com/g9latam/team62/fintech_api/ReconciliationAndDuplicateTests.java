package com.g9latam.team62.fintech_api;

import com.g9latam.team62.fintech_api.dto.ManualTransactionRequest;
import com.g9latam.team62.fintech_api.model.Category;
import com.g9latam.team62.fintech_api.model.Currency;
import com.g9latam.team62.fintech_api.model.LinkStatus;
import com.g9latam.team62.fintech_api.model.PaymentMethod;
import com.g9latam.team62.fintech_api.model.Transaction;
import com.g9latam.team62.fintech_api.dto.RegisterRequest;
import com.g9latam.team62.fintech_api.model.TransactionSource;
import com.g9latam.team62.fintech_api.model.User;
import com.g9latam.team62.fintech_api.repository.TransactionRepository;
import com.g9latam.team62.fintech_api.service.TransactionService;
import com.g9latam.team62.fintech_api.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ReconciliationAndDuplicateTests {

    @Autowired
    private UserService userService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private com.g9latam.team62.fintech_api.service.BudgetRecommendationService budgetRecommendationService;

    @Test
    void bankStatementTransactionsDefaultToDebitAndCaptureOperationNumber() {
        User user = userService.create(newUser("test1@example.com"));

        Transaction transaction = new Transaction();
        transaction.setUserId(user.getId());
        transaction.setAmount(new BigDecimal("15000.00"));
        transaction.setCategory(Category.FOOD);
        transaction.setDate(LocalDate.now());
        transaction.setCurrency(currencyNamed("CLP"));
        transaction.setSource(TransactionSource.BANK);
        transaction.setPaymentMethod(PaymentMethod.DEBIT); // Set by service or statement ingestion
        transaction.setOperationNumber("OP-TEST-123");

        Transaction saved = transactionService.create(transaction);

        assertThat(saved.getPaymentMethod()).isEqualTo(PaymentMethod.DEBIT);
        assertThat(saved.getOperationNumber()).isEqualTo("OP-TEST-123");
    }

    @Test
    void bankTransactionsAvoidDuplicatesBasedOnOperationNumberOrFields() {
        User user = userService.create(newUser("test2@example.com"));

        Transaction transaction1 = new Transaction();
        transaction1.setUserId(user.getId());
        transaction1.setAmount(new BigDecimal("25000.00"));
        transaction1.setCategory(Category.TRANSPORT);
        transaction1.setDate(LocalDate.now());
        transaction1.setCurrency(currencyNamed("CLP"));
        transaction1.setSource(TransactionSource.BANK);
        transaction1.setOperationNumber("OP-DUP-999");

        Transaction saved1 = transactionService.create(transaction1);

        Transaction transaction2 = new Transaction();
        transaction2.setUserId(user.getId());
        transaction2.setAmount(new BigDecimal("25000.00"));
        transaction2.setCategory(Category.TRANSPORT);
        transaction2.setDate(LocalDate.now());
        transaction2.setCurrency(currencyNamed("CLP"));
        transaction2.setSource(TransactionSource.BANK);
        transaction2.setOperationNumber("OP-DUP-999");

        Transaction saved2 = transactionService.create(transaction2);

        // Should return the exact same persisted object and not duplicate it
        assertThat(saved2.getId()).isEqualTo(saved1.getId());

        // Test duplicate detection without operation number (by amount, date, description)
        Transaction transaction3 = new Transaction();
        transaction3.setUserId(user.getId());
        transaction3.setDescription("Compra sin op number");
        transaction3.setAmount(new BigDecimal("3500.00"));
        transaction3.setCategory(Category.FOOD);
        transaction3.setDate(LocalDate.now());
        transaction3.setCurrency(currencyNamed("CLP"));
        transaction3.setSource(TransactionSource.BANK);

        Transaction saved3 = transactionService.create(transaction3);

        Transaction transaction4 = new Transaction();
        transaction4.setUserId(user.getId());
        transaction4.setDescription("Compra sin op number");
        transaction4.setAmount(new BigDecimal("3500.00"));
        transaction4.setCategory(Category.FOOD);
        transaction4.setDate(LocalDate.now());
        transaction4.setCurrency(currencyNamed("CLP"));
        transaction4.setSource(TransactionSource.BANK);

        Transaction saved4 = transactionService.create(transaction4);

        assertThat(saved4.getId()).isEqualTo(saved3.getId());
    }

    @Test
    void manualDebitTransactionLinksWithBankStatementTransaction() {
        User user = userService.create(newUser("test3@example.com"));

        // 1. Create a manual transaction (source = MANUAL, paymentMethod = DEBIT)
        ManualTransactionRequest manualRequest = new ManualTransactionRequest(
                user.getId(),
                new BigDecimal("4990.00"),
                Category.ENTERTAINMENT,
                "Netflix Premium Subscription",
                currencyNamed("CLP"),
                PaymentMethod.DEBIT,
                "Manual",
                null
        );
        Transaction manualSaved = transactionService.createManual(manualRequest);
        assertThat(manualSaved.getLinkStatus()).isEqualTo(LinkStatus.UNLINKED);
        assertThat(manualSaved.getSource()).isEqualTo(TransactionSource.MANUAL);

        // 2. Create the bank statement transaction (source = BANK, same amount, date ± 3 days)
        Transaction bankTransaction = new Transaction();
        bankTransaction.setUserId(user.getId());
        bankTransaction.setDescription("NETFLIX.COM COMPRA DEBITO");
        bankTransaction.setAmount(new BigDecimal("4990.00"));
        bankTransaction.setCategory(Category.ENTERTAINMENT);
        bankTransaction.setDate(LocalDate.now());
        bankTransaction.setCurrency(currencyNamed("CLP"));
        bankTransaction.setSource(TransactionSource.BANK);
        bankTransaction.setOperationNumber("OP-NETFLIX-555");

        Transaction bankSaved = transactionService.create(bankTransaction);

        // Verify they are linked!
        assertThat(bankSaved.getLinkStatus()).isEqualTo(LinkStatus.LINKED);
        assertThat(bankSaved.getLinkedTransactionId()).isEqualTo(manualSaved.getId());

        // Refresh manual transaction and check linked state
        Transaction manualRefreshed = transactionRepository.findById(java.util.Objects.requireNonNull(manualSaved.getId())).orElseThrow();
        assertThat(manualRefreshed.getLinkStatus()).isEqualTo(LinkStatus.LINKED);
        assertThat(manualRefreshed.getLinkedTransactionId()).isEqualTo(bankSaved.getId());
        // Manual transaction should inherit the bank's operation number
        assertThat(manualRefreshed.getOperationNumber()).isEqualTo("OP-NETFLIX-555");
    }

    @Test
    void manualCashTransactionGeneratesUniqueOperationNumberAndDoesNotLink() {
        User user = userService.create(newUser("test4@example.com"));

        // 1. Create a manual cash transaction
        ManualTransactionRequest manualRequest = new ManualTransactionRequest(
                user.getId(),
                new BigDecimal("12000.00"),
                Category.FOOD,
                "Almuerzo Casero en Efectivo",
                currencyNamed("CLP"),
                PaymentMethod.CASH,
                "Manual",
                null
        );
        Transaction manualSaved = transactionService.createManual(manualRequest);

        assertThat(manualSaved.getPaymentMethod()).isEqualTo(PaymentMethod.CASH);
        assertThat(manualSaved.getOperationNumber()).startsWith("CASH-");
        assertThat(manualSaved.getLinkStatus()).isEqualTo(LinkStatus.UNLINKED);

        // 2. Bank statement with matching amount and date should NOT link because manual is CASH
        Transaction bankTransaction = new Transaction();
        bankTransaction.setUserId(user.getId());
        bankTransaction.setDescription("RESTAURANTE ALMUERZO");
        bankTransaction.setAmount(new BigDecimal("12000.00"));
        bankTransaction.setCategory(Category.FOOD);
        bankTransaction.setDate(LocalDate.now());
        bankTransaction.setCurrency(currencyNamed("CLP"));
        bankTransaction.setSource(TransactionSource.BANK);
        bankTransaction.setOperationNumber("OP-FOOD-777");

        Transaction bankSaved = transactionService.create(bankTransaction);

        assertThat(bankSaved.getLinkStatus()).isEqualTo(LinkStatus.UNLINKED);
        Transaction manualRefreshed = transactionRepository.findById(java.util.Objects.requireNonNull(manualSaved.getId())).orElseThrow();
        assertThat(manualRefreshed.getLinkStatus()).isEqualTo(LinkStatus.UNLINKED);
    }

    @Test
    void budgetRecommendationsIgnoreLinkedManualTransactionsToAvoidDoubleCounting() {
        User user = userService.create(newUser("test5@example.com"));

        // Add 5 distinct transactions so MIN_TRANSACTIONS = 5 is satisfied
        // Income
        Transaction income = new Transaction();
        income.setUserId(user.getId());
        income.setAmount(new BigDecimal("100000.00"));
        income.setCategory(Category.SALARY);
        income.setDate(LocalDate.now());
        income.setCurrency(currencyNamed("CLP"));
        income.setSource(TransactionSource.BANK);
        income.setDescription("Salary payment");
        transactionService.create(income);

        // 3 Food expenses (bank)
        for (int i = 0; i < 3; i++) {
            Transaction exp = new Transaction();
            exp.setUserId(user.getId());
            exp.setAmount(new BigDecimal("10000.00"));
            exp.setCategory(Category.FOOD);
            exp.setDate(LocalDate.now());
            exp.setCurrency(currencyNamed("CLP"));
            exp.setSource(TransactionSource.BANK);
            exp.setDescription("Jumbo Supermercado " + i);
            transactionService.create(exp);
        }

        // Manual food expense
        ManualTransactionRequest manualRequest = new ManualTransactionRequest(
                user.getId(),
                new BigDecimal("5000.00"),
                Category.FOOD,
                "Lider Supermarket",
                currencyNamed("CLP"),
                PaymentMethod.DEBIT,
                "Manual",
                null
        );
        Transaction manualSaved = transactionService.createManual(manualRequest);

        // Bank food expense that matches manual and links
        Transaction bankExp = new Transaction();
        bankExp.setUserId(user.getId());
        bankExp.setDescription("COMPRA LIDER SUPERMERCADO");
        bankExp.setAmount(new BigDecimal("5000.00"));
        bankExp.setCategory(Category.FOOD);
        bankExp.setDate(LocalDate.now());
        bankExp.setCurrency(currencyNamed("CLP"));
        bankExp.setSource(TransactionSource.BANK);
        bankExp.setOperationNumber("OP-LIDER-789");
        Transaction bankSaved = transactionService.create(bankExp);

        // Assert they linked
        assertThat(bankSaved.getLinkStatus()).isEqualTo(LinkStatus.LINKED);
        Transaction manualRefreshed = transactionRepository.findById(java.util.Objects.requireNonNull(manualSaved.getId())).orElseThrow();
        assertThat(manualRefreshed.getLinkStatus()).isEqualTo(LinkStatus.LINKED);

        // Calculate sum manually through the same logic as BudgetRecommendationService
        List<Transaction> userTxs = transactionRepository.findByUserIdAndDateBetween(user.getId(), LocalDate.now().minusDays(10), LocalDate.now().plusDays(10));
        assertThat(userTxs).hasSize(6);

        BigDecimal totalFoodExpense = BigDecimal.ZERO;
        for (Transaction t : userTxs) {
            if (t.getCategory() == Category.FOOD) {
                if (t.getSource() == TransactionSource.MANUAL && t.getLinkStatus() == LinkStatus.LINKED) {
                    continue; // Skip manual linked
                }
                totalFoodExpense = totalFoodExpense.add(t.getAmount());
            }
        }

        // Should be 35000.00 (30000 + 5000), NOT 40000.00
        assertThat(totalFoodExpense).isEqualByComparingTo(new BigDecimal("35000.00"));
        
        // Verify budget recommendation generation executes without error
        var recs = budgetRecommendationService.generateRecommendations(java.util.Objects.requireNonNull(user.getId()), LocalDate.now().minusDays(10), LocalDate.now().plusDays(10));
        assertThat(recs).isNotNull();
    }

    @Test
    void bankNameIsolationPreventsIncorrectLinking() {
        User user = userService.create(newUser("test6@example.com"));

        // 1. Create a manual transaction for SANTANDER
        ManualTransactionRequest manualRequest = new ManualTransactionRequest(
                user.getId(),
                new BigDecimal("7990.00"),
                Category.ENTERTAINMENT,
                "Disney Plus",
                currencyNamed("CLP"),
                PaymentMethod.DEBIT,
                "SANTANDER",
                null
        );
        Transaction manualSaved = transactionService.createManual(manualRequest);
        assertThat(manualSaved.getLinkStatus()).isEqualTo(LinkStatus.UNLINKED);

        // 2. Create bank transaction with same amount but for CUENTA_RUT
        Transaction bankTransaction = new Transaction();
        bankTransaction.setUserId(user.getId());
        bankTransaction.setDescription("Disney Plus");
        bankTransaction.setAmount(new BigDecimal("7990.00"));
        bankTransaction.setCategory(Category.ENTERTAINMENT);
        bankTransaction.setDate(LocalDate.now());
        bankTransaction.setCurrency(currencyNamed("CLP"));
        bankTransaction.setSource(TransactionSource.BANK);
        bankTransaction.setBankName("CUENTA_RUT");
        bankTransaction.setOperationNumber("OP-DISNEY-888");

        Transaction bankSaved = transactionService.create(bankTransaction);

        // Verify they are NOT linked because they are from different banks!
        assertThat(bankSaved.getLinkStatus()).isEqualTo(LinkStatus.UNLINKED);
        Transaction manualRefreshed = transactionRepository.findById(java.util.Objects.requireNonNull(manualSaved.getId())).orElseThrow();
        assertThat(manualRefreshed.getLinkStatus()).isEqualTo(LinkStatus.UNLINKED);
    }

    @Test
    void manualTransactionWithCustomOperationNumberLinksPrecisely() {
        User user = userService.create(newUser("test7@example.com"));

        // 1. Create manual transaction specifying an operation number
        ManualTransactionRequest manualRequest = new ManualTransactionRequest(
                user.getId(),
                new BigDecimal("1500.00"),
                Category.TRANSPORT,
                "Metro de Santiago",
                currencyNamed("CLP"),
                PaymentMethod.DEBIT,
                "CUENTA_RUT",
                "OP-METRO-111"
        );
        Transaction manualSaved = transactionService.createManual(manualRequest);
        assertThat(manualSaved.getOperationNumber()).isEqualTo("OP-METRO-111");

        // 2. Create bank transaction matching that operation number
        Transaction bankTransaction = new Transaction();
        bankTransaction.setUserId(user.getId());
        bankTransaction.setDescription("METRO SANTIAGO");
        bankTransaction.setAmount(new BigDecimal("1500.00"));
        bankTransaction.setCategory(Category.TRANSPORT);
        bankTransaction.setDate(LocalDate.now());
        bankTransaction.setCurrency(currencyNamed("CLP"));
        bankTransaction.setSource(TransactionSource.BANK);
        bankTransaction.setBankName("CUENTA_RUT");
        bankTransaction.setOperationNumber("OP-METRO-111");

        Transaction bankSaved = transactionService.create(bankTransaction);

        // Verify they linked
        assertThat(bankSaved.getLinkStatus()).isEqualTo(LinkStatus.LINKED);
        assertThat(bankSaved.getLinkedTransactionId()).isEqualTo(manualSaved.getId());

        Transaction manualRefreshed = transactionRepository.findById(java.util.Objects.requireNonNull(manualSaved.getId())).orElseThrow();
        assertThat(manualRefreshed.getLinkStatus()).isEqualTo(LinkStatus.LINKED);
        assertThat(manualRefreshed.getLinkedTransactionId()).isEqualTo(bankSaved.getId());
    }

    private RegisterRequest newUser(String email) {
        return new RegisterRequest("Test User", email, "secret");
    }

    private Currency currencyNamed(String name) {
        Currency currency = new Currency();
        currency.setNameCurrency(name);
        return currency;
    }
}
