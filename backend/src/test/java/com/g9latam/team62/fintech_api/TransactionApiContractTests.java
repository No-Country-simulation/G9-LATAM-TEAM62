package com.g9latam.team62.fintech_api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.g9latam.team62.fintech_api.dto.ProfileUpdateRequest;
import com.g9latam.team62.fintech_api.dto.RegisterRequest;
import com.g9latam.team62.fintech_api.exception.NotFoundException;
import com.g9latam.team62.fintech_api.model.Category;
import com.g9latam.team62.fintech_api.model.CategoryMethod;
import com.g9latam.team62.fintech_api.model.Currency;
import com.g9latam.team62.fintech_api.model.FinancialProfile;
import com.g9latam.team62.fintech_api.model.LinkStatus;
import com.g9latam.team62.fintech_api.model.SavingFrequency;
import com.g9latam.team62.fintech_api.model.Transaction;
import com.g9latam.team62.fintech_api.model.TransactionSource;
import com.g9latam.team62.fintech_api.model.User;
import com.g9latam.team62.fintech_api.repository.TransactionRepository;
import com.g9latam.team62.fintech_api.security.CustomUserDetailsService;
import com.g9latam.team62.fintech_api.security.JwtService;
import com.g9latam.team62.fintech_api.service.TransactionService;
import com.g9latam.team62.fintech_api.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fija el contrato de la API de transacciones ahora que la entidad dejó de ser el cuerpo de la
 * petición: lo que decide el servidor no se acepta desde afuera ni se pierde al editar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TransactionApiContractTests {

    private static final String EMAIL = "contrato.transacciones@example.com";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserService userService;
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private CustomUserDetailsService userDetailsService;
    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        testUser = userService.findByEmail(EMAIL)
                .orElseGet(() -> userService.create(new RegisterRequest("Contrato", EMAIL, "password")));
        jwtToken = jwtService.generateToken(userDetailsService.loadUserByUsername(EMAIL));
    }

    @Test
    void ignoraLaClasificacionQueDeclareElCliente() throws Exception {
        Map<String, Object> payload = movimiento("COMPRA JUMBO PROVIDENCIA 4410", new BigDecimal("19990"));
        // Un cliente malicioso o descuidado no puede firmar su propia clasificación.
        payload.put("categoryMethod", "EXACT_MAPPING");
        payload.put("categoryConfidence", 1.0);
        payload.put("linkStatus", "LINKED");
        payload.put("linkedTransactionId", 999999);

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryMethod").value("KEYWORD_RULE"))
                .andExpect(jsonPath("$.categoryConfidence").value(0.9))
                .andExpect(jsonPath("$.linkStatus").value("UNLINKED"))
                .andExpect(jsonPath("$.linkedTransactionId").doesNotExist());
    }

    @Test
    void editarUnaTransaccionNoBorraSuEnlaceNiSuTrazabilidad() throws Exception {
        Transaction guardada = transactionService.create(transaccion("PAGO SERVICIOS", new BigDecimal("32000")));
        guardada.setLinkStatus(LinkStatus.LINKED);
        guardada.setLinkedTransactionId(4242L);
        transactionRepository.save(guardada);

        Map<String, Object> payload = movimiento("PAGO SERVICIOS CORREGIDO", new BigDecimal("33000"));

        mockMvc.perform(put("/api/transactions/" + guardada.getId())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("PAGO SERVICIOS CORREGIDO"))
                .andExpect(jsonPath("$.amount").value(33000))
                // el enlace con su contraparte lo mantiene el servidor: la edición no lo menciona
                .andExpect(jsonPath("$.linkStatus").value("LINKED"))
                .andExpect(jsonPath("$.linkedTransactionId").value(4242));
    }

    @Test
    void laCategoriaEnviadaEnUnaEdicionQuedaComoDecisionDelUsuario() throws Exception {
        Transaction guardada = transactionService.create(transaccion("COMPRA JUMBO PROVIDENCIA", new BigDecimal("15000")));
        assertThat(guardada.getCategoryMethod()).isEqualTo(CategoryMethod.KEYWORD_RULE);

        Map<String, Object> payload = movimiento("COMPRA JUMBO PROVIDENCIA", new BigDecimal("15000"));
        payload.put("category", "ENTERTAINMENT");

        mockMvc.perform(put("/api/transactions/" + guardada.getId())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("ENTERTAINMENT"))
                .andExpect(jsonPath("$.categoryMethod").value("USER_PROVIDED"));
    }

    @Test
    void seleccionarMovimientosAjenosNoLosTrae() {
        User otro = userService.create(new RegisterRequest("Ajeno", "ajeno.contrato@example.com", "password"));
        Transaction propia = transactionService.create(transaccion("PROPIA", new BigDecimal("1000")));
        Transaction ajena = transactionService.create(transaccionDe(otro.getId(), "AJENA", new BigDecimal("2000")));

        List<Transaction> seleccion = transactionService.findByUserIdAndIds(
                testUser.getId(), List.of(propia.getId(), ajena.getId()));

        assertThat(seleccion).extracting(Transaction::getId).containsExactly(propia.getId());
    }

    @Test
    void tocarUnUsuarioInexistenteSeReportaComoNoEncontrado() {
        ProfileUpdateRequest perfil = new ProfileUpdateRequest(
                FinancialProfile.SAVER, 0.9, SavingFrequency.MONTHLY);

        // NotFoundException y no IllegalArgumentException: el manejador global traduce la primera
        // a 404, mientras que la segunda respondía 400 a un recurso que simplemente no existe.
        assertThatThrownBy(() -> userService.updateProfile(-1L, perfil))
                .isInstanceOf(NotFoundException.class);
    }

    private Map<String, Object> movimiento(String descripcion, BigDecimal monto) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", testUser.getId());
        payload.put("description", descripcion);
        payload.put("amount", monto);
        payload.put("date", LocalDate.now().toString());
        payload.put("currency", Map.of("name_currency", "CLP"));
        payload.put("source", "BANK");
        payload.put("paymentMethod", "DEBIT");
        return payload;
    }

    private Transaction transaccion(String descripcion, BigDecimal monto) {
        return transaccionDe(testUser.getId(), descripcion, monto);
    }

    private Transaction transaccionDe(Long userId, String descripcion, BigDecimal monto) {
        Transaction tx = new Transaction();
        tx.setUserId(userId);
        tx.setDescription(descripcion);
        tx.setAmount(monto);
        tx.setDate(LocalDate.now());
        tx.setCurrency(new Currency(1L, "CLP"));
        tx.setSource(TransactionSource.MANUAL);
        tx.setCategory(descripcion.contains("JUMBO") ? null : Category.UTILITIES);
        return tx;
    }
}
