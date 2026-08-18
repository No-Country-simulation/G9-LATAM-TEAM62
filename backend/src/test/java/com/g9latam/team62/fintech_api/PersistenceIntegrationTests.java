package com.g9latam.team62.fintech_api;

import com.g9latam.team62.fintech_api.dto.RegisterRequest;
import com.g9latam.team62.fintech_api.dto.TransactionResponse;
import com.g9latam.team62.fintech_api.exception.ConflictException;
import com.g9latam.team62.fintech_api.model.Category;
import com.g9latam.team62.fintech_api.model.Currency;
import com.g9latam.team62.fintech_api.model.Recommendation;
import com.g9latam.team62.fintech_api.model.Transaction;
import com.g9latam.team62.fintech_api.model.TransactionType;
import com.g9latam.team62.fintech_api.model.SavingFrequency;
import com.g9latam.team62.fintech_api.model.User;
import com.g9latam.team62.fintech_api.repository.RecommendationRepository;
import com.g9latam.team62.fintech_api.repository.TransactionRepository;
import com.g9latam.team62.fintech_api.service.RecommendationService;
import com.g9latam.team62.fintech_api.service.StatementIngestionService;
import com.g9latam.team62.fintech_api.service.TransactionService;
import com.g9latam.team62.fintech_api.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Exercises the JPA layer end to end on the dev profile's H2 database. The same
// mappings run on Oracle, where the schema comes from db/oracle/schema.sql.
@SpringBootTest
@Transactional
class PersistenceIntegrationTests {

    @Autowired
    private UserService userService;
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private RecommendationService recommendationService;
    @Autowired
    private StatementIngestionService statementIngestionService;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private RecommendationRepository recommendationRepository;

    @Test
    void ingestsCartolaStatementAndExtractsOperationNumbersAndCLPCurrency() throws Exception {
        User user = userService.create(newUser("statement_tester@example.com"));
        java.io.File cartolaFile = new java.io.File("../Cartola.xlsx");
        if (!cartolaFile.exists()) {
            cartolaFile = new java.io.File("Cartola.xlsx");
        }
        if (cartolaFile.exists()) {
            byte[] content = java.nio.file.Files.readAllBytes(cartolaFile.toPath());
            org.springframework.mock.web.MockMultipartFile multipartFile = new org.springframework.mock.web.MockMultipartFile(
                    "file", "Cartola.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content);
            
            com.g9latam.team62.fintech_api.dto.StatementIngestionResult result =
                    statementIngestionService.ingestStatement(multipartFile, user.getId(), 2026, "CL");
            assertThat(result.status()).isEqualTo("ok");
            assertThat(result.createdTransactions()).isNotEmpty();
            
            TransactionResponse firstWithOp = result.createdTransactions().stream()
                    .filter(t -> "PAGO FERNANDOVASCO".equals(t.description()))
                    .findFirst()
                    .orElse(null);

            assertThat(firstWithOp).isNotNull();
            assertThat(firstWithOp.operationNumber()).isEqualTo("8073913");
            assertThat(firstWithOp.currency().nameCurrency()).isEqualTo("CLP");
            assertThat(firstWithOp.balanceAfter()).isEqualByComparingTo("30852");
            assertThat(firstWithOp.bankName()).isEqualTo("CUENTA_RUT");
        }
    }

    @Test
    void storesAUserWithAHashedPasswordAndFindsItByEmailIgnoringCase() {
        User saved = userService.create(newUser("Ada@example.com"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getPassword()).isNotEqualTo("secret");
        assertThat(userService.authenticate("ADA@example.com", "secret")).isNotNull();
        assertThatThrownBy(() -> userService.authenticate("ada@example.com", "wrong"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Credenciales inválidas");
    }

    @Test
    void rejectsAnEmailAlreadyTakenByAnotherUser() {
        userService.create(newUser("ada@example.com"));

        assertThatThrownBy(() -> userService.create(newUser("ADA@EXAMPLE.COM")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void resolvesTheCurrencyOfATransactionByNameAndDerivesItsType() {
        User user = userService.create(newUser("ada@example.com"));

        Transaction saved = transactionService.create(newTransaction(user.getId(), currencyNamed("clp")));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCurrency().getId()).isNotNull();
        assertThat(saved.getCurrency().getNameCurrency()).isEqualTo("CLP");
        assertThat(saved.getType()).isEqualTo(TransactionType.EXPENSE);
    }

    @Test
    void rejectsATransactionWhoseCurrencyIsNotRegistered() {
        User user = userService.create(newUser("ada@example.com"));

        assertThatThrownBy(() -> transactionService.create(newTransaction(user.getId(), currencyNamed("BTC"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void authenticatingNonExistentUserThrowsSpecificErrorFallback() {
        String email = "non.existent.user@example.com";
        String password = "securePassword123";

        assertThatThrownBy(() -> userService.authenticate(email, password))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Credenciales inválidas");
    }

    @Test
    void deletingAUserRemovesItsTransactionsAndRecommendations() {
        User user = userService.create(newUser("ada@example.com"));
        transactionService.create(newTransaction(user.getId(), currencyNamed("CLP")));
        Recommendation recommendation = new Recommendation();
        recommendation.setText("Aumentar la reserva mensual");
        recommendation.setUserId(user.getId());
        recommendationService.create(recommendation);
        userService.delete(java.util.Objects.requireNonNull(user.getId()));

        assertThat(userService.findById(java.util.Objects.requireNonNull(user.getId()))).isEmpty();
        assertThat(transactionRepository.findByUserId(user.getId())).isEmpty();
        assertThat(recommendationRepository.findByUserId(user.getId())).isEmpty();
    }

    @Test
    void storesAUserWithMonthlyIncomeAndSavingFrequency() {
        RegisterRequest request = new RegisterRequest(
                "Sebas",
                "sebas@example.com",
                "Password123!",
                new BigDecimal("1200000.00"),
                SavingFrequency.MONTHLY
        );
        User saved = userService.create(request);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getMonthlyIncome()).isEqualByComparingTo("1200000.00");
        assertThat(saved.getSavingFrequency()).isEqualTo(SavingFrequency.MONTHLY);

        User retrieved = userService.findById(saved.getId()).orElseThrow();
        assertThat(retrieved.getMonthlyIncome()).isEqualByComparingTo("1200000.00");
        assertThat(retrieved.getSavingFrequency()).isEqualTo(SavingFrequency.MONTHLY);
    }

    private RegisterRequest newUser(String email) {
        return new RegisterRequest("Ada", email, "secret");
    }

    private Transaction newTransaction(Long userId, Currency currency) {
        Transaction transaction = new Transaction();
        transaction.setAmount(new BigDecimal("42.50"));
        transaction.setCategory(Category.FOOD);
        transaction.setDate(LocalDate.now());
        transaction.setCurrency(currency);
        transaction.setUserId(userId);
        return transaction;
    }

    // clients send a reference, not a full row: {"name_currency": "CLP"}
    private Currency currencyNamed(String name) {
        Currency currency = new Currency();
        currency.setNameCurrency(name);
        return currency;
    }
}
