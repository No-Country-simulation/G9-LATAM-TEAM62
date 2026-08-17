package com.g9latam.team62.fintech_api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.g9latam.team62.fintech_api.dto.FinancialAnalysisRequest;
import com.g9latam.team62.fintech_api.dto.RawTransactionDTO;
import com.g9latam.team62.fintech_api.dto.RegisterRequest;
import com.g9latam.team62.fintech_api.model.*;
import com.g9latam.team62.fintech_api.repository.CategoryBudgetTargetRepository;
import com.g9latam.team62.fintech_api.repository.CategoryKeywordRepository;
import com.g9latam.team62.fintech_api.repository.FinancialProfileHistoryRepository;
import com.g9latam.team62.fintech_api.repository.RecommendationRepository;
import com.g9latam.team62.fintech_api.repository.TransactionRepository;
import com.g9latam.team62.fintech_api.security.CustomUserDetailsService;
import com.g9latam.team62.fintech_api.security.JwtService;
import com.g9latam.team62.fintech_api.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@SuppressWarnings("null")
class FinancialAnalysisControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private FinancialProfileHistoryRepository historyRepository;

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private CategoryKeywordRepository keywordRepository;

    @Autowired
    private CategoryBudgetTargetRepository budgetTargetRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private Principal principal;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        String email = "test.analysis@example.com";
        testUser = userService.findByEmail(email)
                .orElseGet(() -> userService.create(new RegisterRequest("Test User", email, "password")));
        principal = () -> email;

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        jwtToken = jwtService.generateToken(userDetails);

        // Seed keywords for Nlevel classification
        keywordRepository.save(new CategoryKeyword(null, "JUMBO", Category.FOOD));
        keywordRepository.save(new CategoryKeyword(null, "UBER", Category.TRANSPORT));

        // Seed budget targets for INE Chile
        budgetTargetRepository.save(new CategoryBudgetTarget(null, Category.FOOD, new BigDecimal("21.3"), "CL", "Alimentacion"));
        budgetTargetRepository.save(new CategoryBudgetTarget(null, Category.TRANSPORT, new BigDecimal("14.1"), "CL", "Transporte"));
    }

    @Test
    void performsFinancialAnalysisSuccessfullyAndUpdatesUserAndSavesHistory() throws Exception {
        RawTransactionDTO tx1 = new RawTransactionDTO("Jumbo Supermercado", new BigDecimal("100.00"));
        RawTransactionDTO tx2 = new RawTransactionDTO("Uber viaje", new BigDecimal("50.00"));

        FinancialAnalysisRequest request = new FinancialAnalysisRequest(
                new BigDecimal("1000.00"),
                new BigDecimal("10.00"),
                "Media",
                List.of(tx1, tx2)
        );

        mockMvc.perform(post("/api/analisis-financiero")
                        .header("Authorization", "Bearer " + jwtToken)
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil_financiero").value("Saludable"))
                .andExpect(jsonPath("$.resumen_gastos.alimentacion").value(100.00))
                .andExpect(jsonPath("$.resumen_gastos.transporte").value(50.00))
                .andExpect(jsonPath("$.recomendaciones").isNotEmpty());

        // Verify transactions were saved in H2 database
        var savedTxs = transactionRepository.findByUserId(testUser.getId());
        assertThat(savedTxs).hasSize(2);

        // Verify profile was updated in history
        List<FinancialProfileHistory> history = historyRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId());
        assertThat(history).isNotEmpty();
        assertThat(history.get(0).getFinancialProfile().name()).isEqualTo("SAVER");
    }

    @Test
    void rejectsInvalidAnalysisPayloadWithJakartaValidationMessages() throws Exception {
        // Invalid payload: null income, empty transactions list
        FinancialAnalysisRequest request = new FinancialAnalysisRequest(
                null,
                new BigDecimal("-5.0"), // Negative debt is invalid
                "",
                List.of()
        );

        mockMvc.perform(post("/api/analisis-financiero")
                        .header("Authorization", "Bearer " + jwtToken)
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getsUserProfileHistorySuccessfully() throws Exception {
        // Perform analysis first to create profile history
        performsFinancialAnalysisSuccessfullyAndUpdatesUserAndSavesHistory();

        mockMvc.perform(get("/api/users/" + testUser.getId() + "/profile-history")
                        .header("Authorization", "Bearer " + jwtToken)
                        .principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isNotEmpty());
    }

    @Test
    void getsRecommendationsHistorySuccessfully() throws Exception {
        // Save mock recommendation
        Recommendation rec = new Recommendation();
        rec.setUserId(testUser.getId());
        rec.setText("Monitorear entretenimiento");
        recommendationRepository.save(rec);

        mockMvc.perform(get("/api/recommendations/user/" + testUser.getId())
                        .header("Authorization", "Bearer " + jwtToken)
                        .principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].text").value("Monitorear entretenimiento"));
    }

    @Test
    void deniesAccessWhenUserRequestsAnotherUsersProfileHistory() throws Exception {
        // Request history for ID -999 which is not owned by principal
        mockMvc.perform(get("/api/users/-999/profile-history")
                        .header("Authorization", "Bearer " + jwtToken)
                        .principal(principal))
                .andExpect(status().isForbidden());
    }
}
