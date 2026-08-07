package com.g9latam.team62.fintech_api.repository;

import com.g9latam.team62.fintech_api.model.Category;
import com.g9latam.team62.fintech_api.model.CategoryBudgetTarget;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class CategoryBudgetTargetRepository {

    private final Map<Long, CategoryBudgetTarget> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong();

    public CategoryBudgetTargetRepository() {
        // Semillas INE Chile (2022-2023)
        initSeed("CL", Category.FOOD, new BigDecimal("21.30"), "Alimentos y bebidas no alcohólicas (INE)");
        initSeed("CL", Category.HOUSING, new BigDecimal("14.50"), "Vivienda y servicios principales");
        initSeed("CL", Category.UTILITIES, new BigDecimal("6.20"), "Electricidad, agua, gas y combustibles");
        initSeed("CL", Category.TRANSPORT, new BigDecimal("14.10"), "Transporte y combustibles");
        initSeed("CL", Category.HEALTH, new BigDecimal("7.40"), "Salud y productos farmacéuticos");
        initSeed("CL", Category.EDUCATION, new BigDecimal("6.50"), "Educación y formación");
        initSeed("CL", Category.ENTERTAINMENT, new BigDecimal("5.10"), "Recreación, deporte y cultura");
        initSeed("CL", Category.SHOPPING, new BigDecimal("4.80"), "Vestuario, calzado y equipamiento del hogar");
        initSeed("CL", Category.OTHER_EXPENSE, new BigDecimal("8.00"), "Otros bienes y servicios diversos");
    }

    private void initSeed(String countryCode, Category category, BigDecimal targetPercentage, String description) {
        save(new CategoryBudgetTarget(null, category, targetPercentage, countryCode, description));
    }

    public CategoryBudgetTarget save(CategoryBudgetTarget target) {
        if (target.getId() == null) {
            target.setId(idGenerator.incrementAndGet());
        }
        storage.put(target.getId(), target);
        return target;
    }

    public Collection<CategoryBudgetTarget> findAll() {
        return storage.values();
    }

    public Optional<CategoryBudgetTarget> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    public List<CategoryBudgetTarget> findByCountryCode(String countryCode) {
        return storage.values().stream()
                .filter(target -> countryCode != null && countryCode.equalsIgnoreCase(target.getCountryCode()))
                .toList();
    }

    public List<CategoryBudgetTarget> findByCountryCodeIgnoreCase(String countryCode) {
        return findByCountryCode(countryCode);
    }
}
