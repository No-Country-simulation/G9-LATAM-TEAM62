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
import com.g9latam.team62.fintech_api.model.LinkStatus;
import com.g9latam.team62.fintech_api.model.TransactionSource;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Motor de Recomendaciones Presupuestarias: compara cómo un usuario reparte
 * su gasto entre categorías contra un % de referencia (CategoryBudgetTarget INE Chile)
 * y genera recomendaciones cuando hay un desbalance.
 */
@Service
public class BudgetRecommendationService {

    private static final int MIN_TRANSACTIONS = 5;
    private static final BigDecimal MODERATE_RATIO = new BigDecimal("1.2");
    private static final BigDecimal HIGH_RATIO = new BigDecimal("1.5");
    private static final BigDecimal SEVERE_RATIO = new BigDecimal("2.0");
    private static final int MAX_RECOMMENDATIONS_PER_RUN = 3;
    private static final int COOLDOWN_DAYS = 7;
    private static final BigDecimal TARGET_SAVINGS_RATE = new BigDecimal("0.20"); // 20%
    private static final String DEFAULT_COUNTRY = "CL";

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

    public List<Recommendation> generateRecommendations(@NonNull Long userId, LocalDate periodStart, LocalDate periodEnd) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("El usuario " + userId + " no existe"));

        if (isInCooldown(userId)) {
            return List.of();
        }

        List<Transaction> transactions = transactionRepository.findByUserIdAndDateBetween(userId, periodStart, periodEnd);
        if (transactions.size() < MIN_TRANSACTIONS) {
            return List.of(); // No hay señal suficiente todavía
        }

        Map<Category, BigDecimal> expenseByCategory = sumByCategory(transactions, TransactionType.EXPENSE);
        BigDecimal totalExpense = expenseByCategory.values().stream()
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
        if (totalExpense.signum() == 0) {
            return List.of();
        }

        List<CategoryDeviation> deviations = computeDeviations(expenseByCategory, totalExpense);

        List<Recommendation> created = new ArrayList<>();
        deviations.stream()
                .sorted(Comparator.comparing((CategoryDeviation d) -> d.ratio()).reversed())
                .limit(MAX_RECOMMENDATIONS_PER_RUN)
                .forEach(d -> created.add(save(user, buildCategoryText(d))));

        BigDecimal totalIncome = sumByCategory(transactions, TransactionType.INCOME).values().stream()
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
        buildSavingsTextIfNeeded(totalIncome, totalExpense)
                .ifPresent(text -> created.add(save(user, text)));

        return created;
    }

    @SuppressWarnings("null")
    public List<String> generateRecommendationsStateless(List<Transaction> transactions, BigDecimal monthlyIncome) {
        if (transactions == null || transactions.isEmpty()) {
            return List.of();
        }

        Map<Category, BigDecimal> expenseByCategory = sumByCategory(transactions, TransactionType.EXPENSE);
        BigDecimal totalExpense = expenseByCategory.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalExpense.signum() == 0) {
            return List.of();
        }

        List<CategoryDeviation> deviations = computeDeviations(expenseByCategory, totalExpense);

        List<String> recommendations = new ArrayList<>();
        deviations.stream()
                .sorted(Comparator.comparing((CategoryDeviation d) -> d.ratio()).reversed())
                .limit(MAX_RECOMMENDATIONS_PER_RUN)
                .forEach(d -> recommendations.add(buildCategoryText(d)));

        BigDecimal totalIncome = monthlyIncome != null && monthlyIncome.signum() > 0 ? monthlyIncome :
                sumByCategory(transactions, TransactionType.INCOME).values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        buildSavingsTextIfNeeded(totalIncome, totalExpense)
                .ifPresent(recommendations::add);

        return recommendations;
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
                continue; // Excluye transferencias internas del cálculo de consumo
            }
            // Evitar doble conteo: si una transacción manual está vinculada, se omite porque ya se cuenta la bancaria
            if (t.getSource() == TransactionSource.MANUAL && t.getLinkStatus() == LinkStatus.LINKED) {
                continue;
            }
            totals.merge(t.getCategory(), t.getAmount(), (a, b) -> a.add(b));
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
            if (recommended == null || recommended.signum() <= 0) {
                continue;
            }
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

    private Optional<String> buildSavingsTextIfNeeded(BigDecimal totalIncome, BigDecimal totalExpense) {
        if (totalIncome.signum() <= 0) {
            return Optional.empty();
        }
        BigDecimal savingsRate = totalIncome.subtract(totalExpense)
                .divide(totalIncome, 4, RoundingMode.HALF_UP);
        if (savingsRate.compareTo(TARGET_SAVINGS_RATE) >= 0) {
            return Optional.empty();
        }
        return Optional.of(String.format(
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
