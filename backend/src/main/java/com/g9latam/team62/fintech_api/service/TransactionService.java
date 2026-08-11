package com.g9latam.team62.fintech_api.service;

import com.g9latam.team62.fintech_api.dto.CategoryCorrectionRequest;
import com.g9latam.team62.fintech_api.dto.ClassificationResult;
import com.g9latam.team62.fintech_api.dto.ManualTransactionRequest;
import com.g9latam.team62.fintech_api.model.Category;
import com.g9latam.team62.fintech_api.model.CategoryMethod;
import com.g9latam.team62.fintech_api.model.Currency;
import com.g9latam.team62.fintech_api.model.LinkStatus;
import com.g9latam.team62.fintech_api.model.Transaction;
import com.g9latam.team62.fintech_api.model.TransactionSource;
import com.g9latam.team62.fintech_api.repository.CurrencyRepository;
import com.g9latam.team62.fintech_api.repository.TransactionRepository;
import com.g9latam.team62.fintech_api.repository.UserRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class TransactionService {

    private final TransactionRepository repository;
    private final UserRepository userRepository;
    private final CurrencyRepository currencyRepository;
    private final CategoryClassifierService classifierService;

    public TransactionService(TransactionRepository repository,
                              UserRepository userRepository,
                              CurrencyRepository currencyRepository,
                              CategoryClassifierService classifierService) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.currencyRepository = currencyRepository;
        this.classifierService = classifierService;
    }

    /**
     * Crea una nueva transacción (generalmente bancaria o del pipeline de cartolas).
     * Si la transacción no especifica categoría y posee descripción, se clasifica
     * automáticamente usando el motor jerárquico de 4 niveles.
     */
    @Transactional
    public Transaction create(Transaction transaction) {
        requireUserExists(transaction.getUserId());
        transaction.setCurrency(resolveCurrency(transaction.getCurrency()));
        transaction.setId(null); // Asignado por el repositorio

        if (transaction.getSource() == null) {
            transaction.setSource(TransactionSource.BANK);
        }
        if (transaction.getLinkStatus() == null) {
            transaction.setLinkStatus(LinkStatus.UNLINKED);
        }

        // Clasificación automática si no trae categoría definida o si viene requerida
        if (transaction.getCategory() == null && transaction.getDescription() != null && !transaction.getDescription().isBlank()) {
            ClassificationResult result = classifierService.classify(transaction.getDescription());
            transaction.setCategory(result.category());
            transaction.setCategoryMethod(parseCategoryMethod(result.method()));
            transaction.setCategoryConfidence(result.confidence());
        }

        return repository.save(transaction);
    }

    private CategoryMethod parseCategoryMethod(String methodStr) {
        if (methodStr == null) return CategoryMethod.FALLBACK;
        try {
            return CategoryMethod.valueOf(methodStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return CategoryMethod.FALLBACK;
        }
    }

    /**
     * Crea una transacción manual en efectivo o débito registrada directamente por el usuario.
     */
    @Transactional
    public Transaction createManual(ManualTransactionRequest request) {
        requireUserExists(request.userId());

        Transaction transaction = new Transaction();
        transaction.setUserId(request.userId());
        transaction.setAmount(request.amount());
        transaction.setCategory(request.category());
        transaction.setDescription(request.description());
        transaction.setDate(LocalDate.now());
        Currency currencyRef = request.currency() != null ? request.currency() : new Currency(1L, "CLP");
        transaction.setCurrency(resolveCurrency(currencyRef));
        transaction.setSource(TransactionSource.MANUAL);
        transaction.setPaymentMethod(request.paymentMethod());
        transaction.setCategoryMethod(CategoryMethod.USER_PROVIDED);
        transaction.setLinkStatus(LinkStatus.UNLINKED);
        transaction.setBankName(request.bankName());

        return repository.save(transaction);
    }

    /**
     * Actualiza la categoría de una transacción según la retroalimentación del usuario.
     * Al corregir la categoría, se actualiza la tabla de mapeos colaborativos para que
     * futuras transacciones con la misma descripción se clasifiquen instantáneamente (Nivel 1).
     */
    @Transactional
    public Transaction updateCategory(@NonNull Long id, Category newCategory) {
        Transaction transaction = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La transacción " + id + " no existe"));

        transaction.setCategory(newCategory);
        transaction.setCategoryMethod(CategoryMethod.USER_CORRECTED);
        transaction.setCategoryConfidence(null); // No aplica: fue corrección humana

        // Retroalimentación — actualiza el mapeo colaborativo (Nivel 1)
        if (transaction.getDescription() != null && !transaction.getDescription().isBlank()) {
            classifierService.learnFromFeedback(transaction.getDescription(), newCategory);
        }

        return repository.save(transaction);
    }

    /**
     * Endpoint alternativo de corrección mediante DTO.
     */
    @Transactional
    public Transaction correctCategory(@NonNull Long id, CategoryCorrectionRequest request) {
        return updateCategory(id, request.category());
    }

    public Collection<Transaction> findAll() {
        return repository.findAll();
    }

    public Optional<Transaction> findById(@NonNull Long id) {
        return repository.findById(id);
    }

    public List<Transaction> findByUserId(Long userId) {
        return repository.findByUserId(userId);
    }

    @Transactional
    public Transaction update(@NonNull Long id, Transaction transaction) {
        requireUserExists(transaction.getUserId());
        transaction.setCurrency(resolveCurrency(transaction.getCurrency()));
        transaction.setId(id);
        return repository.save(transaction);
    }

    @Transactional
    public void delete(@NonNull Long id) {
        repository.deleteById(id);
    }

    private void requireUserExists(Long userId) {
        if (userRepository.findById(java.util.Objects.requireNonNull(userId, "userId is required")).isEmpty()) {
            throw new IllegalArgumentException("El usuario " + userId + " no existe");
        }
    }

    // currencies live in their own table, so the payload only carries a reference:
    // either {"id": 1} or {"name_currency": "CLP"}. Anything else is a bad request.
    private Currency resolveCurrency(Currency currency) {
        if (currency == null) {
            throw new IllegalArgumentException("currency is required");
        }
        if (currency.getId() != null) {
            return currencyRepository.findById(currency.getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "currency " + currency.getId() + " does not exist"));
        }
        String name = currency.getNameCurrency();
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("currency must carry an id or a name_currency");
        }
        return currencyRepository.findByNameCurrencyIgnoreCase(name.trim())
                .orElseThrow(() -> new IllegalArgumentException("currency " + name + " does not exist"));
    }
}
