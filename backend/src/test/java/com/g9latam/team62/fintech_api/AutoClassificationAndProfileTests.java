package com.g9latam.team62.fintech_api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.g9latam.team62.fintech_api.dto.RegisterRequest;
import com.g9latam.team62.fintech_api.model.Category;
import com.g9latam.team62.fintech_api.model.CategoryMethod;
import com.g9latam.team62.fintech_api.model.Currency;
import com.g9latam.team62.fintech_api.model.SavingFrequency;
import com.g9latam.team62.fintech_api.model.Transaction;
import com.g9latam.team62.fintech_api.model.TransactionSource;
import com.g9latam.team62.fintech_api.model.User;
import com.g9latam.team62.fintech_api.repository.FinancialProfileHistoryRepository;
import com.g9latam.team62.fintech_api.security.CustomUserDetailsService;
import com.g9latam.team62.fintech_api.security.JwtService;
import com.g9latam.team62.fintech_api.service.TransactionService;
import com.g9latam.team62.fintech_api.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regresiones de la clasificación automática y del guardado del perfil.
 *
 * <p><b>Sin {@code @Transactional} a propósito.</b> Un test transaccional comparte la sesión de
 * persistencia con el controlador, así que las entidades que este obtiene quedan gestionadas y
 * cualquier escritura sobre ellas termina guardándose igual. En producción no hay transacción
 * ambiental: la entidad viene desligada y esas escrituras se pierden. Ese es exactamente el fallo
 * que {@link #guardaElIngresoMensualDelAnalisis()} vigila, y con {@code @Transactional} el test
 * pasaría con el código roto. El precio es limpiar a mano en {@link #limpiar()}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AutoClassificationAndProfileTests {

    private static final String EMAIL = "regresiones.clasificacion@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private FinancialProfileHistoryRepository historyRepository;

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
                .orElseGet(() -> userService.create(new RegisterRequest("Regresiones", EMAIL, "password")));

        UserDetails userDetails = userDetailsService.loadUserByUsername(EMAIL);
        jwtToken = jwtService.generateToken(userDetails);
    }

    @AfterEach
    void limpiar() {
        historyRepository.deleteAll(historyRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId()));
        userService.delete(testUser.getId());
    }

    // ── F-05 ─────────────────────────────────────────────────────────────────

    @Test
    void guardaElIngresoMensualDelAnalisis() throws Exception {
        ejecutarAnalisis("mensual", new BigDecimal("1200000"), "COMPRA JUMBO PROVIDENCIA", new BigDecimal("150000"));

        User actualizado = userService.findById(testUser.getId()).orElseThrow();
        assertThat(actualizado.getMonthlyIncome())
                .as("el ingreso que originó el perfil debe quedar registrado, no solo el perfil")
                .isEqualByComparingTo("1200000");
    }

    // ── F-12: "Ocasionalmente" caía al valor por defecto ──────────────────────

    @Test
    void traduceOcasionalmenteAAhorroEsporadico() throws Exception {
        ejecutarAnalisis("Ocasionalmente", new BigDecimal("900000"), "UBER TRIP 4471", new BigDecimal("7000"));

        User actualizado = userService.findById(testUser.getId()).orElseThrow();
        assertThat(actualizado.getSavingFrequency()).isEqualTo(SavingFrequency.RARELY);
    }

    // ── Montos inválidos ─────────────────────────────────────────────────────
    //
    // El caso "la cartola trae el cargo en negativo" dejó de pasar por aquí: desde b29e8a3
    // el análisis solo lee movimientos ya guardados y quien normaliza el signo es
    // StatementIngestionService.processAndSaveTransactions, con Math.abs sobre el monto del
    // parser. Lo que sigue vigente en la API es que un monto cero no se acepta.

    @Test
    void rechazaMontoCero() throws Exception {
        Map<String, Object> payload = transaccionSinCategoria("MOVIMIENTO NULO SIN MONTO", BigDecimal.ZERO);

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    // ── F-08: crear sin categoría debe clasificar, no rechazar ───────────────

    @Test
    void clasificaUnaTransaccionCreadaSinCategoria() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transaccionSinCategoria(
                                "COMPRA JUMBO PROVIDENCIA 1234", new BigDecimal("15990")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("FOOD"))
                .andExpect(jsonPath("$.categoryMethod").value("KEYWORD_RULE"))
                .andExpect(jsonPath("$.categoryConfidence").value(0.9));
    }

    @Test
    void respetaLaCategoriaCuandoElUsuarioSiLaEnvia() throws Exception {
        Map<String, Object> payload = transaccionSinCategoria("COMPRA JUMBO PROVIDENCIA 5678", new BigDecimal("16990"));
        payload.put("category", "ENTERTAINMENT");

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("ENTERTAINMENT"))
                .andExpect(jsonPath("$.categoryMethod").value("USER_PROVIDED"));
    }

    @Test
    void usaElFallbackCuandoNoHayCategoriaNiDescripcion() {
        Transaction sinDatos = new Transaction();
        sinDatos.setUserId(testUser.getId());
        sinDatos.setAmount(new BigDecimal("3300"));
        sinDatos.setDate(LocalDate.now());
        sinDatos.setCurrency(new Currency(1L, "CLP"));
        sinDatos.setSource(TransactionSource.MANUAL);

        Transaction creada = transactionService.create(sinDatos);

        assertThat(creada.getCategory()).isEqualTo(Category.OTHER_EXPENSE);
        assertThat(creada.getCategoryMethod()).isEqualTo(CategoryMethod.FALLBACK);
    }

    // ── F-06: un acierto de nivel 1 se guardaba como FALLBACK ────────────────

    @Test
    void registraElAprendizajeColaborativoComoExactMapping() throws Exception {
        String descripcion = "ZZQQ COMERCIO SIN PALABRA CLAVE";

        // 1. Sin coincidencias: cae al fallback.
        Transaction original = transactionService.create(
                transaccion(descripcion, new BigDecimal("31000")));
        assertThat(original.getCategoryMethod()).isEqualTo(CategoryMethod.FALLBACK);

        // 2. El usuario corrige la categoría, lo que alimenta el mapeo colaborativo.
        mockMvc.perform(put("/api/transactions/" + original.getId() + "/category")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"FOOD\"}"))
                .andExpect(status().isOk());

        // 3. La siguiente con la misma descripción se resuelve en el nivel 1.
        Transaction aprendida = transactionService.create(
                transaccion(descripcion, new BigDecimal("32000")));

        assertThat(aprendida.getCategory()).isEqualTo(Category.FOOD);
        assertThat(aprendida.getCategoryMethod())
                .as("un acierto de nivel 1 no puede quedar etiquetado como FALLBACK")
                .isEqualTo(CategoryMethod.EXACT_MAPPING);
        assertThat(aprendida.getCategoryConfidence()).isEqualTo(1.0);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /**
     * Deja el movimiento guardado y luego lanza el análisis sobre él. El endpoint ya no crea
     * transacciones desde el cuerpo de la petición: analiza las que el usuario tiene en la base
     * de datos, todas o las que se nombren en {@code transaction_ids}.
     */
    private void ejecutarAnalisis(String frecuenciaAhorro, BigDecimal ingreso,
                                  String descripcion, BigDecimal valor) throws Exception {
        Transaction analizada = transactionService.create(transaccion(descripcion, valor));

        mockMvc.perform(post("/api/analisis-financiero")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "ingreso_mensual", ingreso,
                                "nivel_endeudamiento", new BigDecimal("10"),
                                "frecuencia_ahorro", frecuenciaAhorro,
                                "transaction_ids", List.of(analizada.getId())))))
                .andExpect(status().isOk());
    }

    private Transaction transaccion(String descripcion, BigDecimal monto) {
        Transaction tx = new Transaction();
        tx.setUserId(testUser.getId());
        tx.setDescription(descripcion);
        tx.setAmount(monto);
        tx.setDate(LocalDate.now());
        tx.setCurrency(new Currency(1L, "CLP"));
        tx.setSource(TransactionSource.MANUAL);
        return tx;
    }

    private Map<String, Object> transaccionSinCategoria(String descripcion, BigDecimal monto) {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("userId", testUser.getId());
        payload.put("description", descripcion);
        payload.put("amount", monto);
        payload.put("date", LocalDate.now().toString());
        payload.put("currency", Map.of("id", 1));
        payload.put("source", "BANK");
        payload.put("paymentMethod", "DEBIT");
        payload.put("linkStatus", "UNLINKED");
        return payload;
    }
}
