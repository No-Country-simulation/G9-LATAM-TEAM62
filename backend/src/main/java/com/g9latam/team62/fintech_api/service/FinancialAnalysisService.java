package com.g9latam.team62.fintech_api.service;

import com.g9latam.team62.fintech_api.dto.FinancialAnalysisRequest;
import com.g9latam.team62.fintech_api.dto.FinancialAnalysisResponse;
import com.g9latam.team62.fintech_api.dto.ProfileUpdateRequest;
import com.g9latam.team62.fintech_api.exception.NotFoundException;
import com.g9latam.team62.fintech_api.model.Category;
import com.g9latam.team62.fintech_api.model.FinancialProfile;
import com.g9latam.team62.fintech_api.model.Recommendation;
import com.g9latam.team62.fintech_api.model.SavingFrequency;
import com.g9latam.team62.fintech_api.model.Transaction;
import com.g9latam.team62.fintech_api.model.TransactionType;
import com.g9latam.team62.fintech_api.model.User;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Diagnóstico presupuestario del usuario: clasifica su perfil según lo que ya tiene registrado,
 * lo guarda con su historial y deja las recomendaciones generadas.
 *
 * <p>Vive fuera del controlador por la frontera transaccional. Cuando estos pasos se ejecutaban
 * sueltos desde la capa web, la actualización del perfil confirmaba por su cuenta y cada
 * recomendación se guardaba en su propia transacción: un fallo a mitad de camino dejaba el perfil
 * nuevo con solo parte de las recomendaciones. Aquí, o entra todo o no entra nada.
 */
@Service
public class FinancialAnalysisService {

    /** Proporción de gasto sobre ingreso a partir de la cual el perfil se considera en riesgo. */
    private static final BigDecimal AT_RISK_RATIO = new BigDecimal("0.90");
    private static final BigDecimal SPENDER_RATIO = new BigDecimal("0.70");
    private static final BigDecimal BALANCED_RATIO = new BigDecimal("0.40");

    /** Nivel de endeudamiento (en porcentaje) que basta por sí solo para cada perfil. */
    private static final BigDecimal AT_RISK_DEBT = new BigDecimal("40.0");
    private static final BigDecimal SPENDER_DEBT = new BigDecimal("25.0");

    private final UserService userService;
    private final TransactionService transactionService;
    private final BudgetRecommendationService budgetRecommendationService;
    private final RecommendationService recommendationService;

    public FinancialAnalysisService(UserService userService,
                                    TransactionService transactionService,
                                    BudgetRecommendationService budgetRecommendationService,
                                    RecommendationService recommendationService) {
        this.userService = userService;
        this.transactionService = transactionService;
        this.budgetRecommendationService = budgetRecommendationService;
        this.recommendationService = recommendationService;
    }

    @Transactional
    public FinancialAnalysisResponse analyze(@NonNull Long userId, FinancialAnalysisRequest request) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new NotFoundException("El usuario " + userId + " no existe"));

        BigDecimal income = resolveIncome(request, user);
        SavingFrequency savingFrequency = resolveSavingFrequency(request, user);
        BigDecimal debt = request.nivelEndeudamiento() != null ? request.nivelEndeudamiento() : BigDecimal.ZERO;

        List<Transaction> analyzed = selectTransactions(userId, request);
        Map<Category, BigDecimal> expensesByCategory = summariseExpenses(analyzed);
        Profiling profiling = classify(totalExpense(expensesByCategory), income, debt);

        // El perfil se guarda antes que las recomendaciones para que estas queden asociadas al
        // perfil recién calculado y no al anterior.
        userService.updateProfile(userId, new ProfileUpdateRequest(
                profiling.profile(), profiling.accuracy(), savingFrequency, income));

        List<String> advice = adviseFor(analyzed, income, profiling.profile());
        advice.forEach(text -> persist(userId, text));

        return FinancialAnalysisResponse.of(profiling.profile(), profiling.accuracy(), expensesByCategory, advice);
    }

    /** Un ingreso ausente o negativo deja el que el usuario ya tenía registrado. */
    private BigDecimal resolveIncome(FinancialAnalysisRequest request, User user) {
        if (request.ingresoMensual() != null && request.ingresoMensual().signum() >= 0) {
            return request.ingresoMensual();
        }
        return user.getMonthlyIncome() != null ? user.getMonthlyIncome() : BigDecimal.ZERO;
    }

    private SavingFrequency resolveSavingFrequency(FinancialAnalysisRequest request, User user) {
        if (request.frecuenciaAhorro() != null && !request.frecuenciaAhorro().isBlank()) {
            return SavingFrequency.fromText(request.frecuenciaAhorro());
        }
        return user.getSavingFrequency() != null ? user.getSavingFrequency() : SavingFrequency.MONTHLY;
    }

    /**
     * Los movimientos a analizar salen de la base de datos: o los que el usuario seleccionó, o
     * todos los suyos. La consulta filtra por usuario además de por id, así que pedir ids ajenos
     * no expone nada; simplemente no aparecen en el análisis.
     */
    private List<Transaction> selectTransactions(Long userId, FinancialAnalysisRequest request) {
        List<Long> requestedIds = request.transactionIds();
        if (requestedIds == null || requestedIds.isEmpty()) {
            return transactionService.findByUserId(userId);
        }
        return transactionService.findByUserIdAndIds(userId, requestedIds);
    }

    private Map<Category, BigDecimal> summariseExpenses(List<Transaction> transactions) {
        Map<Category, BigDecimal> totals = new EnumMap<>(Category.class);
        for (Transaction transaction : transactions) {
            Category category = transaction.getCategory();
            if (category != null && category.getType() == TransactionType.EXPENSE) {
                totals.merge(category, transaction.getAmount(), BigDecimal::add);
            }
        }
        return totals;
    }

    private BigDecimal totalExpense(Map<Category, BigDecimal> expensesByCategory) {
        return expensesByCategory.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Profiling classify(BigDecimal totalExpense, BigDecimal income, BigDecimal debt) {
        BigDecimal expenseRatio = income.compareTo(BigDecimal.ZERO) > 0
                ? totalExpense.divide(income, 4, RoundingMode.HALF_UP)
                : BigDecimal.ONE;

        if (expenseRatio.compareTo(AT_RISK_RATIO) >= 0 || debt.compareTo(AT_RISK_DEBT) >= 0) {
            return new Profiling(FinancialProfile.AT_RISK, 0.90);
        }
        if (expenseRatio.compareTo(SPENDER_RATIO) >= 0 || debt.compareTo(SPENDER_DEBT) >= 0) {
            return new Profiling(FinancialProfile.SPENDER, 0.85);
        }
        if (expenseRatio.compareTo(BALANCED_RATIO) >= 0) {
            return new Profiling(FinancialProfile.BALANCED, 0.80);
        }
        return new Profiling(FinancialProfile.SAVER, 0.95);
    }

    private List<String> adviseFor(List<Transaction> transactions, BigDecimal income, FinancialProfile profile) {
        List<String> advice = new ArrayList<>(
                budgetRecommendationService.generateRecommendationsStateless(transactions, income));
        if (advice.isEmpty()) {
            advice.add(defaultAdvice(profile));
        }
        return advice;
    }

    private String defaultAdvice(FinancialProfile profile) {
        return switch (profile) {
            case AT_RISK -> "Reducir urgentemente los gastos no esenciales para evitar sobreendeudamiento.";
            case SPENDER -> "Considerar la posibilidad de automatizar tu ahorro al recibir tus ingresos.";
            default -> "Buen control de tus gastos: continúa manteniendo tu margen de ahorro regular.";
        };
    }

    private void persist(Long userId, String text) {
        Recommendation recommendation = new Recommendation();
        recommendation.setUserId(userId);
        recommendation.setText(text);
        recommendationService.create(recommendation);
    }

    /** Perfil calculado junto con la confianza que le corresponde. */
    private record Profiling(FinancialProfile profile, double accuracy) {}
}
