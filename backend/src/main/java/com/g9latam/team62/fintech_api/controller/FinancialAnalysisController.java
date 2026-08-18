package com.g9latam.team62.fintech_api.controller;

import com.g9latam.team62.fintech_api.dto.FinancialAnalysisRequest;
import com.g9latam.team62.fintech_api.dto.FinancialAnalysisResponse;
import com.g9latam.team62.fintech_api.dto.ProfileUpdateRequest;
import com.g9latam.team62.fintech_api.model.*;
import com.g9latam.team62.fintech_api.model.Currency;
import com.g9latam.team62.fintech_api.repository.RecommendationRepository;
import com.g9latam.team62.fintech_api.repository.TransactionRepository;
import com.g9latam.team62.fintech_api.security.AuthorizationHelper;
import com.g9latam.team62.fintech_api.service.BudgetRecommendationService;
import com.g9latam.team62.fintech_api.service.TransactionService;
import com.g9latam.team62.fintech_api.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.Principal;
import java.time.LocalDate;
import java.util.*;

@RestController
@Tag(name = "Análisis Financiero", description = "Endpoints para el análisis financiero y diagnóstico presupuestario del usuario")
public class FinancialAnalysisController {

    private final UserService userService;
    private final TransactionService transactionService;
    private final TransactionRepository transactionRepository;
    private final RecommendationRepository recommendationRepository;
    private final BudgetRecommendationService budgetRecommendationService;
    private final AuthorizationHelper authorizationHelper;

    public FinancialAnalysisController(UserService userService,
                                       TransactionService transactionService,
                                       TransactionRepository transactionRepository,
                                       RecommendationRepository recommendationRepository,
                                       BudgetRecommendationService budgetRecommendationService,
                                       AuthorizationHelper authorizationHelper) {
        this.userService = userService;
        this.transactionService = transactionService;
        this.transactionRepository = transactionRepository;
        this.recommendationRepository = recommendationRepository;
        this.budgetRecommendationService = budgetRecommendationService;
        this.authorizationHelper = authorizationHelper;
    }

    @PostMapping({"/api/analisis-financiero", "/analisis-financiero"})
    @Operation(summary = "Analizar el comportamiento financiero del usuario",
               description = "Analiza los movimientos registrados en la base de datos (todas las transacciones del usuario o una lista de IDs seleccionados), actualiza el perfil y genera recomendaciones presupuestarias.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Análisis completado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = FinancialAnalysisResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "401", description = "No autenticado o token JWT inválido")
    })
    @SuppressWarnings("null")
    public ResponseEntity<FinancialAnalysisResponse> analyze(
            @Valid @RequestBody FinancialAnalysisRequest request, Principal principal) {

        // 1. Authn: Obtener el usuario autenticado
        User user = authorizationHelper.getAuthenticatedUser(principal);
        Long userId = user.getId();

        // 2. Actualizar datos del perfil del usuario según la request (con fallback a valores guardados)
        BigDecimal income = (request.ingresoMensual() != null && request.ingresoMensual().signum() >= 0)
                ? request.ingresoMensual()
                : (user.getMonthlyIncome() != null ? user.getMonthlyIncome() : BigDecimal.ZERO);

        SavingFrequency savingFreq = (request.frecuenciaAhorro() != null && !request.frecuenciaAhorro().isBlank())
                ? parseSavingFrequency(request.frecuenciaAhorro())
                : (user.getSavingFrequency() != null ? user.getSavingFrequency() : SavingFrequency.MONTHLY);

        BigDecimal debt = request.nivelEndeudamiento() != null ? request.nivelEndeudamiento() : BigDecimal.ZERO;

        user.setMonthlyIncome(income);
        user.setSavingFrequency(savingFreq);

        // 3. Obtener las transacciones objetivo a analizar desde la base de datos
        List<Transaction> targetTransactions = new ArrayList<>();
        Map<String, BigDecimal> resumenGastosMap = new HashMap<>();

        if (request.transactionIds() != null && !request.transactionIds().isEmpty()) {
            // Caso A: IDs específicos seleccionados por el usuario desde la BD
            Set<Long> selectedIds = new HashSet<>(request.transactionIds());
            List<Transaction> userTxs = transactionRepository.findByUserId(userId);
            for (Transaction tx : userTxs) {
                if (selectedIds.contains(tx.getId())) {
                    targetTransactions.add(tx);
                }
            }
        } else {
            // Caso B: Todas las transacciones existentes en BD para este usuario
            targetTransactions.addAll(transactionRepository.findByUserId(userId));
        }

        // Resumir gastos únicamente (tipo EXPENSE) para el bloque resumen_gastos
        for (Transaction tx : targetTransactions) {
            if (tx.getCategory() != null && tx.getCategory().getType() == TransactionType.EXPENSE) {
                String label = translateCategory(tx.getCategory());
                resumenGastosMap.put(label, resumenGastosMap.getOrDefault(label, BigDecimal.ZERO).add(tx.getAmount()));
            }
        }

        // 4. Determinar el perfil financiero (`SAVER`, `BALANCED`, `SPENDER`, `AT_RISK`)
        BigDecimal totalExpense = targetTransactions.stream()
                .filter(t -> t.getCategory() != null && t.getCategory().getType() == TransactionType.EXPENSE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expenseRatio = income.compareTo(BigDecimal.ZERO) > 0
                ? totalExpense.divide(income, 4, RoundingMode.HALF_UP)
                : BigDecimal.ONE;

        FinancialProfile profile;
        BigDecimal accuracy;

        if (expenseRatio.compareTo(new BigDecimal("0.90")) >= 0 || debt.compareTo(new BigDecimal("40.0")) >= 0) {
            profile = FinancialProfile.AT_RISK;
            accuracy = new BigDecimal("0.90");
        } else if (expenseRatio.compareTo(new BigDecimal("0.70")) >= 0 || debt.compareTo(new BigDecimal("25.0")) >= 0) {
            profile = FinancialProfile.SPENDER;
            accuracy = new BigDecimal("0.85");
        } else if (expenseRatio.compareTo(new BigDecimal("0.40")) >= 0) {
            profile = FinancialProfile.BALANCED;
            accuracy = new BigDecimal("0.80");
        } else {
            profile = FinancialProfile.SAVER;
            accuracy = new BigDecimal("0.95");
        }

        // 5. Actualizar el perfil en BD (User + FinancialProfileHistory para auditoría y trazabilidad)
        userService.updateProfile(userId, new ProfileUpdateRequest(profile, accuracy, savingFreq));

        // 6. Generar las recomendaciones utilizando la lógica stateless de benchmarking INE Chile
        List<String> recomendaciones = budgetRecommendationService.generateRecommendationsStateless(targetTransactions, income);

        // Si la lista de recomendaciones está vacía, aseguramos al menos algunos consejos por defecto según el perfil
        if (recomendaciones.isEmpty()) {
            if (profile == FinancialProfile.AT_RISK) {
                recomendaciones.add("Reducir urgentemente los gastos no esenciales para evitar sobreendeudamiento.");
            } else if (profile == FinancialProfile.SPENDER) {
                recomendaciones.add("Considerar la posibilidad de automatizar tu ahorro al recibir tus ingresos.");
            } else {
                recomendaciones.add("Buen control de tus gastos: continúa manteniendo tu margen de ahorro regular.");
            }
        }

        // Guardar recomendaciones generadas en el historial de la base de datos
        for (String recText : recomendaciones) {
            Recommendation rec = new Recommendation();
            rec.setUserId(userId);
            rec.setText(recText);
            rec.setGeneratedAt(java.time.LocalDateTime.now());
            rec.setProfileAtGeneration(profile);
            recommendationRepository.save(rec);
        }

        // Traducir el perfil financiero para el formato JSON de salida esperado por el caso de estudio
        String perfilSalida = translateProfile(profile);

        // 7. Retornar respuesta
        FinancialAnalysisResponse response = new FinancialAnalysisResponse(
                perfilSalida,
                accuracy,
                resumenGastosMap,
                recomendaciones
        );

        return ResponseEntity.ok(response);
    }

    private SavingFrequency parseSavingFrequency(String text) {
        if (text == null) return SavingFrequency.MONTHLY;
        String clean = text.trim().toUpperCase();
        if (clean.contains("ALTA") || clean.contains("HIGH") || clean.contains("SEMANAL")) return SavingFrequency.WEEKLY;
        if (clean.contains("MEDIA") || clean.contains("MEDIUM") || clean.contains("MENSUAL")) return SavingFrequency.MONTHLY;
        if (clean.contains("BAJA") || clean.contains("LOW") || clean.contains("RARA")) return SavingFrequency.RARELY;
        if (clean.contains("NINGUNA") || clean.contains("NUNCA") || clean.contains("NEVER")) return SavingFrequency.NEVER;
        try {
            return SavingFrequency.valueOf(clean);
        } catch (IllegalArgumentException e) {
            return SavingFrequency.MONTHLY;
        }
    }

    private String translateProfile(FinancialProfile profile) {
        return switch (profile) {
            case SAVER -> "Saludable";
            case BALANCED, SPENDER -> "En observación";
            case AT_RISK -> "En riesgo";
        };
    }

    private String translateCategory(Category category) {
        return switch (category) {
            case FOOD -> "alimentacion";
            case TRANSPORT -> "transporte";
            case HOUSING -> "vivienda";
            case UTILITIES -> "servicios";
            case ENTERTAINMENT -> "entretenimiento";
            case HEALTH -> "salud";
            case EDUCATION -> "educacion";
            case SHOPPING -> "compras";
            default -> "otros_gastos";
        };
    }
}
