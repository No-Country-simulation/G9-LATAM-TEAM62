package com.g9latam.team62.fintech_api.service;

import com.g9latam.team62.fintech_api.model.Category;
import com.g9latam.team62.fintech_api.model.CategoryBudgetTarget;
import com.g9latam.team62.fintech_api.model.Recommendation;
import com.g9latam.team62.fintech_api.model.Transaction;
import com.g9latam.team62.fintech_api.model.TransactionType;
import com.g9latam.team62.fintech_api.model.User;
import com.g9latam.team62.fintech_api.repository.CategoryBudgetTargetRepository;
import com.g9latam.team62.fintech_api.repository.RecommendationRepository;
import com.g9latam.team62.fintech_api.repository.TransactionRepository;
import com.g9latam.team62.fintech_api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

// Motor de Recomendaciones Presupuestarias: compara cómo un usuario reparte
// su gasto entre categorías contra un % de referencia (CategoryBudgetTarget)
// y genera recomendaciones cuando hay un desbalance. Es aritmética simple
// sobre datos ya categorizados -- no hay Machine Learning acá a propósito.
//
// IMPORTANTE: este servicio se adapta a Category.java / TransactionType.java
// tal como existen hoy en el backend real -- no requiere agregar ninguna
// categoría ni tipo nuevo. Las transferencias (TEF, giros) quedan guardadas
// con la categoría que les haya asignado el clasificador (normalmente
// OTHER_EXPENSE u OTHER_INCOME, según dirección) y se excluyen del cálculo
// de % de gasto acá, detectándolas por texto en la descripción -- el dinero
// se sigue registrando igual, solo no cuenta como "gasto de consumo".
@Service
@Transactional(readOnly = true)
public class BudgetRecommendationService {

    // --- parámetros del motor, ajustables sin tocar la lógica ---
    private static final int MIN_TRANSACTIONS = 5;              // bajo esto, no hay señal suficiente
    private static final BigDecimal MODERATE_RATIO = new BigDecimal("1.2");
    private static final BigDecimal HIGH_RATIO = new BigDecimal("1.5");
    private static final BigDecimal SEVERE_RATIO = new BigDecimal("2.0");
    private static final int MAX_RECOMMENDATIONS_PER_RUN = 3;    // no abrumar al usuario
    private static final int COOLDOWN_DAYS = 7;                  // no repetir en menos de esto
    private static final BigDecimal TARGET_SAVINGS_RATE = new BigDecimal("0.20"); // 20%
    private static final String DEFAULT_COUNTRY = "CL";

    // mismas palabras clave que TRANSFER_KEYWORDS en budget_recommendation_engine.py
    // y que la categoría "TRANSACTIONS" del notebook -- una transferencia sigue
    // siendo un egreso real, pero no es un gasto de consumo, así que se excluye
    // acá aunque haya quedado guardada como OTHER_EXPENSE/OTHER_INCOME.
    private static final Pattern TRANSFER_PATTERN =
            Pattern.compile("\\b(TRANSF|TEF|GIRO|TRANSFERENCIA)\\b", Pattern.CASE_INSENSITIVE);

    private final TransactionRepository transactionRepository;
    private final CategoryBudgetTargetRepository budgetTargetRepository;
    private final RecommendationRepository recommendationRepository;
    private final UserRepository userRepository;

    public BudgetRecommendationService(TransactionRepository transactionRepository,
                                        CategoryBudgetTargetRepository budgetTargetRepository,
                                        RecommendationRepository recommendationRepository,
                                        UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.budgetTargetRepository = budgetTargetRepository;
        this.recommendationRepository = recommendationRepository;
        this.userRepository = userRepository;
    }

    /**
     * Analiza las transacciones del usuario en [periodStart, periodEnd], compara
     * contra los porcentajes de referencia, y persiste hasta
     * MAX_RECOMMENDATIONS_PER_RUN recomendaciones nuevas (más, opcionalmente,
     * una de tasa de ahorro). Devuelve la lista de lo que efectivamente se creó
     * -- puede ser vacía si no hay datos suficientes o si el usuario está en
     * cooldown.
     */
    @Transactional
    public List<Recommendation> generateRecommendations(Long userId, LocalDate periodStart, LocalDate periodEnd) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user " + userId + " does not exist"));

        if (isInCooldown(userId)) {
            return List.of();
        }

        List<Transaction> transactions = transactionRepository.findByUserIdAndDateBetween(userId, periodStart, periodEnd);
        if (transactions.size() < MIN_TRANSACTIONS) {
            return List.of(); // no hay señal suficiente todavía
        }

        Map<Category, BigDecimal> expenseByCategory = sumByCategory(transactions, TransactionType.EXPENSE);
        BigDecimal totalExpense = expenseByCategory.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalExpense.signum() == 0) {
            return List.of();
        }

        List<CategoryDeviation> deviations = computeDeviations(expenseByCategory, totalExpense);

        List<Recommendation> created = new ArrayList<>();
        deviations.stream()
                .sorted(Comparator.comparing(CategoryDeviation::ratio).reversed())
                .limit(MAX_RECOMMENDATIONS_PER_RUN)
                .forEach(d -> created.add(save(user, buildCategoryText(d))));

        BigDecimal totalIncome = sumByCategory(transactions, TransactionType.INCOME).values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        buildSavingsTextIfNeeded(totalIncome, totalExpense)
                .ifPresent(text -> created.add(save(user, text)));

        return created;
    }

    private boolean isInCooldown(Long userId) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(COOLDOWN_DAYS);
        return recommendationRepository.findByUserId(userId).stream()
                .anyMatch(r -> r.getGeneratedAt() != null && r.getGeneratedAt().isAfter(cutoff));
    }

    private Map<Category, BigDecimal> sumByCategory(List<Transaction> transactions, TransactionType type) {
        Map<Category, BigDecimal> totals = new EnumMap<>(Category.class);
        for (Transaction t : transactions) {
            if (t.getCategory() == null || t.getCategory().getType() != type) {
                continue;
            }
            if (isLikelyTransfer(t)) {
                continue; // egreso/ingreso real, pero no es gasto/ingreso de consumo
            }
            totals.merge(t.getCategory(), t.getAmount(), BigDecimal::add);
        }
        return totals;
    }

    private boolean isLikelyTransfer(Transaction t) {
        return t.getDescription() != null && TRANSFER_PATTERN.matcher(t.getDescription()).find();
    }

    private List<CategoryDeviation> computeDeviations(Map<Category, BigDecimal> expenseByCategory, BigDecimal totalExpense) {
        List<CategoryBudgetTarget> targets = budgetTargetRepository.findByCountryCode(DEFAULT_COUNTRY);
        List<CategoryDeviation> deviations = new ArrayList<>();

        for (CategoryBudgetTarget target : targets) {
            BigDecimal categoryTotal = expenseByCategory.getOrDefault(target.getCategory(), BigDecimal.ZERO);
            BigDecimal actualPercentage = categoryTotal
                    .multiply(BigDecimal.valueOf(100))
                    .divide(totalExpense, 4, RoundingMode.HALF_UP);
            BigDecimal recommended = target.getRecommendedPercentage();
            BigDecimal ratio = actualPercentage.divide(recommended, 4, RoundingMode.HALF_UP);

            if (ratio.compareTo(MODERATE_RATIO) >= 0) {
                deviations.add(new CategoryDeviation(target.getCategory(), actualPercentage, recommended, ratio));
            }
        }
        return deviations;
    }

    private String buildCategoryText(CategoryDeviation d) {
        String severity = severityLabel(d.ratio());
        return String.format(
                "Gasto %s en %s: %.1f%% de tu gasto total (referencia: %.1f%%). Considera revisar los gastos recurrentes en esta categoría.",
                severity, translate(d.category()), d.actualPercentage(), d.recommendedPercentage());
    }

    private String severityLabel(BigDecimal ratio) {
        if (ratio.compareTo(SEVERE_RATIO) >= 0) {
            return "muy por sobre lo recomendado";
        }
        if (ratio.compareTo(HIGH_RATIO) >= 0) {
            return "bastante por sobre lo recomendado";
        }
        return "levemente por sobre lo recomendado";
    }

    private java.util.Optional<String> buildSavingsTextIfNeeded(BigDecimal totalIncome, BigDecimal totalExpense) {
        if (totalIncome.signum() <= 0) {
            return java.util.Optional.empty(); // sin ingresos registrados en el período, no hay tasa que calcular
        }
        BigDecimal savingsRate = totalIncome.subtract(totalExpense)
                .divide(totalIncome, 4, RoundingMode.HALF_UP);
        if (savingsRate.compareTo(TARGET_SAVINGS_RATE) >= 0) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(String.format(
                "Tu tasa de ahorro este período fue de %.1f%%, bajo el objetivo de %.0f%%. Aumentar la frecuencia de ahorro puede ayudarte a construir un margen frente a imprevistos.",
                savingsRate.multiply(BigDecimal.valueOf(100)), TARGET_SAVINGS_RATE.multiply(BigDecimal.valueOf(100))));
    }

    private Recommendation save(User user, String text) {
        Recommendation recommendation = new Recommendation();
        recommendation.setUserId(user.getId());
        recommendation.setText(text);
        recommendation.setGeneratedAt(LocalDateTime.now());
        recommendation.setProfileAtGeneration(user.getFinancialProfile());
        return recommendationRepository.save(recommendation);
    }

    // Traducciones simples solo para el texto de la recomendación; el nombre
    // "oficial" de la categoría sigue siendo el del enum en toda la API.
    private String translate(Category category) {
        return switch (category) {
            case FOOD -> "alimentación";
            case TRANSPORT -> "transporte";
            case HOUSING -> "vivienda";
            case UTILITIES -> "servicios básicos";
            case ENTERTAINMENT -> "entretenimiento";
            case HEALTH -> "salud";
            case EDUCATION -> "educación";
            case SHOPPING -> "compras";
            case OTHER_EXPENSE -> "otros gastos";
            default -> category.name();
        };
    }

    private record CategoryDeviation(Category category, BigDecimal actualPercentage,
                                      BigDecimal recommendedPercentage, BigDecimal ratio) {
    }
}
