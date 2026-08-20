package com.g9latam.team62.fintech_api.service;

import com.g9latam.team62.fintech_api.model.Category;
import com.g9latam.team62.fintech_api.model.Recommendation;
import com.g9latam.team62.fintech_api.model.Transaction;
import com.g9latam.team62.fintech_api.model.TransactionType;
import com.g9latam.team62.fintech_api.model.User;
import com.g9latam.team62.fintech_api.repository.RecommendationRepository;
import com.g9latam.team62.fintech_api.repository.TransactionRepository;
import com.g9latam.team62.fintech_api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

// Evolutivo del cliente: compara el % de gasto del mes actual, por categoría,
// contra el PROPIO promedio histórico del usuario -- no contra el % fijo del
// INE (eso ya lo hace BudgetRecommendationService, que sigue funcionando
// igual, sin cambios). Estadística simple (media y desviación estándar), no
// Machine Learning -- mismo criterio que el resto del motor.
//
// PENDIENTE DE VALIDAR CON DATOS REALES: los parámetros de abajo se
// ajustaron empíricamente contra un dataset dummy con ruido gaussiano
// controlado + anomalías inyectadas a propósito (ver
// data-science/evolutivo/ para el generador y la validación completa).
// Resultado sobre 20 usuarios simulados: 10/10 anomalías detectadas, 0
// falsos positivos. Es un punto de partida razonable, no una verdad
// definitiva -- conviene re-validar apenas haya 3+ meses de cartolas reales
// cargadas por usuario.
@Service
@Transactional(readOnly = true)
public class ClientEvolutionService {

    private static final int MIN_MONTHS_HISTORY = 3;
    private static final BigDecimal MIN_STD_DEVIATIONS = new BigDecimal("2.0");
    private static final BigDecimal MIN_DEVIATION_FLOOR_PP = new BigDecimal("4.0");

    private static final Pattern TRANSFER_PATTERN =
            Pattern.compile("\\b(TRANSF|TEF|GIRO|TRANSFERENCIA)\\b", Pattern.CASE_INSENSITIVE);

    private final TransactionRepository transactionRepository;
    private final RecommendationRepository recommendationRepository;
    private final UserRepository userRepository;

    public ClientEvolutionService(TransactionRepository transactionRepository,
                                   RecommendationRepository recommendationRepository,
                                   UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.recommendationRepository = recommendationRepository;
        this.userRepository = userRepository;
    }

    /**
     * Compara el mes `evaluationMonth` contra los `lookbackMonths` anteriores.
     * Si no hay al menos MIN_MONTHS_HISTORY meses previos con datos, devuelve
     * lista vacía -- no hay suficiente historia para calcular "el promedio de
     * esta persona" todavía.
     */
    @Transactional
    public List<Recommendation> generateEvolutionRecommendations(Long userId, YearMonth evaluationMonth,
                                                                   int lookbackMonths) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user " + userId + " does not exist"));

        LocalDate windowStart = evaluationMonth.minusMonths(lookbackMonths).atDay(1);
        LocalDate windowEnd = evaluationMonth.atEndOfMonth();
        List<Transaction> transactions = transactionRepository.findByUserIdAndDateBetween(userId, windowStart, windowEnd);

        Map<YearMonth, Map<Category, BigDecimal>> byMonth = groupByMonthAndCategory(transactions);

        Map<Category, BigDecimal> currentMonthData = byMonth.getOrDefault(evaluationMonth, Map.of());
        BigDecimal currentTotal = sum(currentMonthData.values());
        if (currentTotal.signum() == 0) {
            return List.of(); // sin gasto este mes, nada que comparar
        }

        List<YearMonth> historyMonths = byMonth.keySet().stream()
                .filter(m -> !m.equals(evaluationMonth))
                .toList();
        if (historyMonths.size() < MIN_MONTHS_HISTORY) {
            return List.of(); // no hay suficiente historia todavía
        }

        Map<Category, List<BigDecimal>> historicalPercentages = buildHistoricalPercentages(byMonth, historyMonths);

        List<Recommendation> created = new ArrayList<>();
        for (Map.Entry<Category, BigDecimal> entry : currentMonthData.entrySet()) {
            Category category = entry.getKey();
            BigDecimal currentPct = percentage(entry.getValue(), currentTotal);

            List<BigDecimal> historicos = historicalPercentages.get(category);
            if (historicos == null || historicos.isEmpty()) {
                continue; // esta categoría nunca apareció en el historial, no hay con qué comparar
            }

            BigDecimal mean = mean(historicos);
            BigDecimal stdDev = stdDev(historicos, mean).max(MIN_DEVIATION_FLOOR_PP);
            BigDecimal threshold = mean.add(stdDev.multiply(MIN_STD_DEVIATIONS));

            if (currentPct.compareTo(threshold) > 0) {
                BigDecimal diff = currentPct.subtract(mean);
                created.add(save(user, buildText(category, currentPct, mean, diff)));
            }
        }

        return created;
    }

    private Map<YearMonth, Map<Category, BigDecimal>> groupByMonthAndCategory(List<Transaction> transactions) {
        Map<YearMonth, Map<Category, BigDecimal>> byMonth = new TreeMap<>();
        for (Transaction t : transactions) {
            if (t.getCategory() == null || t.getCategory().getType() != TransactionType.EXPENSE) {
                continue;
            }
            if (isLikelyTransfer(t)) {
                continue; // mismo criterio que BudgetRecommendationService: egreso real, no gasto de consumo
            }
            YearMonth month = YearMonth.from(t.getDate());
            byMonth.computeIfAbsent(month, k -> new EnumMap<>(Category.class))
                    .merge(t.getCategory(), t.getAmount(), BigDecimal::add);
        }
        return byMonth;
    }

    private Map<Category, List<BigDecimal>> buildHistoricalPercentages(Map<YearMonth, Map<Category, BigDecimal>> byMonth,
                                                                        List<YearMonth> historyMonths) {
        Map<Category, List<BigDecimal>> result = new EnumMap<>(Category.class);
        for (YearMonth month : historyMonths) {
            Map<Category, BigDecimal> monthData = byMonth.get(month);
            BigDecimal monthTotal = sum(monthData.values());
            if (monthTotal.signum() == 0) {
                continue;
            }
            for (Map.Entry<Category, BigDecimal> e : monthData.entrySet()) {
                result.computeIfAbsent(e.getKey(), k -> new ArrayList<>())
                        .add(percentage(e.getValue(), monthTotal));
            }
        }
        return result;
    }

    private boolean isLikelyTransfer(Transaction t) {
        return t.getDescription() != null && TRANSFER_PATTERN.matcher(t.getDescription()).find();
    }

    private BigDecimal sum(java.util.Collection<BigDecimal> values) {
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal percentage(BigDecimal part, BigDecimal total) {
        return part.multiply(BigDecimal.valueOf(100)).divide(total, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal mean(List<BigDecimal> values) {
        return sum(values).divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal stdDev(List<BigDecimal> values, BigDecimal mean) {
        if (values.size() < 2) {
            return BigDecimal.ZERO;
        }
        BigDecimal sumSquares = values.stream()
                .map(v -> v.subtract(mean).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal variance = sumSquares.divide(BigDecimal.valueOf(values.size() - 1), 6, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));
    }

    private String buildText(Category category, BigDecimal currentPct, BigDecimal mean, BigDecimal diff) {
        return String.format(
                "Este mes gastaste %.0f%% en %s, %.0f puntos por sobre tu propio promedio de los últimos meses (%.0f%%). "
                        + "Puede valer la pena revisar si fue un gasto puntual o un cambio de hábito.",
                currentPct, translate(category), diff, mean);
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
}
