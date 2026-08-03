package com.g9latam.team62.fintech_api.service;

import com.g9latam.team62.fintech_api.dto.ClassificationResult;
import com.g9latam.team62.fintech_api.model.Category;
import com.g9latam.team62.fintech_api.model.Transaction;
import com.g9latam.team62.fintech_api.repository.TransactionRepository;
import com.g9latam.team62.fintech_api.repository.UserRepository;
import org.springframework.stereotype.Service;

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
     * Crea una nueva transacción. Si la transacción incluye descripción,
     * la categoría se clasifica automáticamente mediante el motor jerárquico de 4 niveles.
     */
    public Transaction create(Transaction transaction) {
        requireUserExists(transaction.getUserId());
        transaction.setId(null); // Los IDs son asignados por el repositorio

        // Clasificación automática si existe descripción
        if (transaction.getDescription() != null && !transaction.getDescription().isBlank()) {
            ClassificationResult result = classifierService.classify(transaction.getDescription());
            transaction.setCategory(result.category());
        }

        return repository.save(transaction);
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

    /**
     * Actualiza la categoría de una transacción según la retroalimentación del usuario.
     * Al corregir la categoría, se actualiza la tabla de mapeos colaborativos para que
     * futuras transacciones con la misma descripción se clasifiquen instantáneamente (Nivel 1).
     *
     * @param id          ID de la transacción
     * @param newCategory nueva categoría elegida por el usuario
     * @return la transacción actualizada
     * @throws IllegalArgumentException si la transacción no existe
     */
    public Transaction updateCategory(Long id, Category newCategory) {
        Transaction transaction = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La transacción " + id + " no existe"));

        // Actualiza la categoría de la transacción
        transaction.setCategory(newCategory);

        // Retroalimentación — actualiza el mapeo colaborativo
        if (transaction.getDescription() != null) {
            classifierService.learnFromFeedback(transaction.getDescription(), newCategory);
        }

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
