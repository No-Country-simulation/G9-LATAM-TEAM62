package com.g9latam.team62.fintech_api.service;

import com.g9latam.team62.fintech_api.dto.CategoryCorrectionRequest;
import com.g9latam.team62.fintech_api.dto.ManualTransactionRequest;
import com.g9latam.team62.fintech_api.model.CategoryMethod;
import com.g9latam.team62.fintech_api.model.Currency;
import com.g9latam.team62.fintech_api.model.LinkStatus;
import com.g9latam.team62.fintech_api.model.Transaction;
import com.g9latam.team62.fintech_api.model.TransactionSource;
import com.g9latam.team62.fintech_api.repository.CurrencyRepository;
import com.g9latam.team62.fintech_api.repository.TransactionRepository;
import com.g9latam.team62.fintech_api.repository.UserRepository;
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

    public TransactionService(TransactionRepository repository, UserRepository userRepository,
                              CurrencyRepository currencyRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.currencyRepository = currencyRepository;
    }

    @Transactional
    public Transaction create(Transaction transaction) {
        requireUserExists(transaction.getUserId());
        transaction.setCurrency(resolveCurrency(transaction.getCurrency()));
        transaction.setId(null); // ids are assigned by the repository, never by the client
        transaction.setSource(TransactionSource.BANK);
        // -- aporte -- si category viene null (transacción recién ingerida desde
        // una cartola, antes de clasificar), este es el punto donde debería
        // engancharse CategoryClassifierService.classify(description) --
        // mapeo -> regla -> modelo -> fallback -- seteando category +
        // categoryMethod + categoryConfidence según el nivel que la resolvió.
        // No implementado acá a propósito: es responsabilidad del servicio de
        // clasificación, no de este CRUD.
        return repository.save(transaction);
    }

    // -- aporte -- Alternativa 1 (registro manual). A propósito NO acepta
    // operationNumber ni balanceAfter del cliente: esos campos no existen o
    // no le corresponden a una entrada que no vino de un banco. La fecha
    // siempre es "hoy", asignada por el servidor.
    @Transactional
    public Transaction createManual(ManualTransactionRequest request) {
        requireUserExists(request.userId());

        Transaction transaction = new Transaction();
        transaction.setUserId(request.userId());
        transaction.setAmount(request.amount());
        transaction.setCategory(request.category());
        transaction.setDescription(request.description());
        transaction.setDate(LocalDate.now());
        transaction.setCurrency(resolveCurrency(request.currency()));
        transaction.setSource(TransactionSource.MANUAL);
        transaction.setPaymentMethod(request.paymentMethod());
        transaction.setCategoryMethod(CategoryMethod.USER_PROVIDED);
        // queda UNLINKED hasta que corra el job de conciliación contra la cartola;
        // si el medio de pago es EFECTIVO, se queda así para siempre (nunca hay
        // nada contra qué conciliarlo)
        transaction.setLinkStatus(LinkStatus.UNLINKED);

        return repository.save(transaction);
    }

    // -- aporte -- Feedback del usuario: corrige la categoría sugerida por el
    // pipeline de clasificación (mapeo / regla / modelo). Ojo: este método NO
    // actualiza transaction_category_mappings ni dispara el reentrenamiento
    // por sí mismo -- eso es responsabilidad de
    // CategoryClassifierService.learnFromFeedback (equipo de Backend); acá
    // solo dejamos la transacción en un estado correcto y trazable.
    @Transactional
    public Transaction correctCategory(Long id, CategoryCorrectionRequest request) {
        Transaction transaction = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("transaction " + id + " does not exist"));
        transaction.setCategory(request.category());
        transaction.setCategoryMethod(CategoryMethod.USER_CORRECTED);
        transaction.setCategoryConfidence(null); // ya no aplica: fue una corrección humana, no una predicción
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

    @Transactional
    public Transaction update(Long id, Transaction transaction) {
        requireUserExists(transaction.getUserId());
        transaction.setCurrency(resolveCurrency(transaction.getCurrency()));
        transaction.setId(id);
        return repository.save(transaction);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    private void requireUserExists(Long userId) {
        if (userRepository.findById(userId).isEmpty()) {
            throw new IllegalArgumentException("user " + userId + " does not exist");
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
