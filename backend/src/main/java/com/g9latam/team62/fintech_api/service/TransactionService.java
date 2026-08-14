package com.g9latam.team62.fintech_api.service;

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
import com.g9latam.team62.fintech_api.model.PaymentMethod;
import java.util.UUID;

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

        // 1. Duplication Prevention (for BANK statements)
        if (transaction.getSource() == TransactionSource.BANK) {
            List<Transaction> existingBankTxs;
            if (transaction.getOperationNumber() != null && !transaction.getOperationNumber().isBlank()) {
                existingBankTxs = repository.findByUserIdAndOperationNumberAndSource(transaction.getUserId(), transaction.getOperationNumber(), TransactionSource.BANK);
            } else {
                existingBankTxs = repository.findByUserIdAndAmountAndDateAndDescriptionAndSource(
                        transaction.getUserId(),
                        transaction.getAmount(),
                        transaction.getDate(),
                        transaction.getDescription(),
                        TransactionSource.BANK
                );
            }
            if (!existingBankTxs.isEmpty()) {
                // Return existing to skip duplication
                return existingBankTxs.get(0);
            }

            // 2. Link with Manual transactions (DEBIT/CREDIT, not CASH)
            List<Transaction> candidates = repository.findCandidates(
                    transaction.getUserId(),
                    TransactionSource.MANUAL,
                    LinkStatus.UNLINKED,
                    transaction.getAmount(),
                    transaction.getDate().minusDays(3),
                    transaction.getDate().plusDays(3)
            );

            Transaction bestMatch = null;
            for (Transaction candidate : candidates) {
                if (candidate.getPaymentMethod() == PaymentMethod.CASH) {
                    continue;
                }
                // Verify bankName matches if both specify it
                if (transaction.getBankName() != null && candidate.getBankName() != null) {
                    if (!transaction.getBankName().equalsIgnoreCase(candidate.getBankName())) {
                        continue; // Different banks, skip
                    }
                }
                if (candidate.getOperationNumber() != null && transaction.getOperationNumber() != null) {
                    if (candidate.getOperationNumber().equalsIgnoreCase(transaction.getOperationNumber())) {
                        bestMatch = candidate;
                        break;
                    }
                } else if (isDescriptionSimilar(transaction.getDescription(), candidate.getDescription())) {
                    bestMatch = candidate;
                } else if (bestMatch == null) {
                    // Fallback to first non-CASH candidate
                    bestMatch = candidate;
                }
            }

            if (bestMatch != null) {
                transaction.setLinkStatus(LinkStatus.LINKED);
                Transaction savedBank = repository.save(transaction);

                bestMatch.setLinkStatus(LinkStatus.LINKED);
                bestMatch.setLinkedTransactionId(savedBank.getId());
                if (bestMatch.getOperationNumber() == null || bestMatch.getOperationNumber().isBlank()) {
                    bestMatch.setOperationNumber(savedBank.getOperationNumber());
                }
                repository.save(bestMatch);

                savedBank.setLinkedTransactionId(bestMatch.getId());
                return repository.save(savedBank);
            }
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

        // 1. CASH transactions get a unique sequence number and bypass linking
        if (request.paymentMethod() == PaymentMethod.CASH) {
            if (request.operationNumber() != null && !request.operationNumber().isBlank()) {
                transaction.setOperationNumber(request.operationNumber());
            } else {
                String shortUuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                transaction.setOperationNumber("CASH-" + shortUuid);
            }
            return repository.save(transaction);
        }

        // Set operation number if sent by the request (e.g. manually entered)
        if (request.operationNumber() != null && !request.operationNumber().isBlank()) {
            transaction.setOperationNumber(request.operationNumber());
        }

        // 2. Non-CASH (DEBIT/CREDIT) manual transactions try to link with existing unlinked BANK transactions
        List<Transaction> candidates = repository.findCandidates(
                transaction.getUserId(),
                TransactionSource.BANK,
                LinkStatus.UNLINKED,
                transaction.getAmount(),
                transaction.getDate().minusDays(3),
                transaction.getDate().plusDays(3)
        );

        Transaction bestMatch = null;
        for (Transaction candidate : candidates) {
            // Verify bankName matches if both specify it
            if (transaction.getBankName() != null && candidate.getBankName() != null) {
                if (!transaction.getBankName().equalsIgnoreCase(candidate.getBankName())) {
                    continue; // Different banks, skip
                }
            }
            // Prioritize matching by operationNumber if specified in the manual transaction
            if (transaction.getOperationNumber() != null && candidate.getOperationNumber() != null) {
                if (transaction.getOperationNumber().equalsIgnoreCase(candidate.getOperationNumber())) {
                    bestMatch = candidate;
                    break;
                }
            } else if (isDescriptionSimilar(transaction.getDescription(), candidate.getDescription())) {
                bestMatch = candidate;
                break;
            } else if (bestMatch == null) {
                bestMatch = candidate;
            }
        }

        if (bestMatch != null) {
            transaction.setLinkStatus(LinkStatus.LINKED);
            transaction.setOperationNumber(bestMatch.getOperationNumber());
            Transaction savedManual = repository.save(transaction);

            bestMatch.setLinkStatus(LinkStatus.LINKED);
            bestMatch.setLinkedTransactionId(savedManual.getId());
            repository.save(bestMatch);

            savedManual.setLinkedTransactionId(bestMatch.getId());
            return repository.save(savedManual);
        }

        return repository.save(transaction);
    }

    private boolean isDescriptionSimilar(String desc1, String desc2) {
        if (desc1 == null || desc2 == null) {
            return false;
        }
        String d1 = desc1.toLowerCase().replaceAll("[^a-z0-9]", " ").trim();
        String d2 = desc2.toLowerCase().replaceAll("[^a-z0-9]", " ").trim();
        if (d1.isEmpty() || d2.isEmpty()) {
            return false;
        }
        return d1.contains(d2) || d2.contains(d1);
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
            return currencyRepository.findById(java.util.Objects.requireNonNull(currency.getId()))
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
