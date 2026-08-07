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
import com.g9latam.team62.fintech_api.repository.TransactionRepository;
import com.g9latam.team62.fintech_api.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {

    private final TransactionRepository repository;
    private final UserRepository userRepository;
    private final CategoryClassifierService classifierService;

    public TransactionService(TransactionRepository repository,
                              UserRepository userRepository,
                              CategoryClassifierService classifierService) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.classifierService = classifierService;
    }

    /**
     * Crea una nueva transacción (generalmente bancaria o del pipeline de cartolas).
     * Si la transacción no especifica categoría y posee descripción, se clasifica
     * automáticamente usando el motor jerárquico de 4 niveles.
     */
    public Transaction create(Transaction transaction) {
        requireUserExists(transaction.getUserId());
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
    public Transaction createManual(ManualTransactionRequest request) {
        requireUserExists(request.userId());

        Transaction transaction = new Transaction();
        transaction.setUserId(request.userId());
        transaction.setAmount(request.amount());
        transaction.setCategory(request.category());
        transaction.setDescription(request.description());
        transaction.setDate(LocalDate.now());
        transaction.setCurrency(request.currency() != null ? request.currency() : new Currency(1L, "CLP"));
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
    public Transaction updateCategory(Long id, Category newCategory) {
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
    public Transaction correctCategory(Long id, CategoryCorrectionRequest request) {
        return updateCategory(id, request.category());
    }

    public Collection<Transaction> findAll() {
        return repository.findAll();
    }

    public Optional<Transaction> findById(Long id) {
        return repository.findById(id);
    }

    public List<Transaction> findByUserId(Long userId) {
        return repository.findByUserId(userId);
    }

    public Transaction update(Long id, Transaction transaction) {
        requireUserExists(transaction.getUserId());
        transaction.setId(id);
        return repository.save(transaction);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    private void requireUserExists(Long userId) {
        if (userRepository.findById(userId).isEmpty()) {
            throw new IllegalArgumentException("El usuario " + userId + " no existe");
        }
    }
}
