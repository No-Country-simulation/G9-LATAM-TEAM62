package com.g9latam.team62.fintech_api.service;

import com.g9latam.team62.fintech_api.model.Currency;
import com.g9latam.team62.fintech_api.model.Transaction;
import com.g9latam.team62.fintech_api.repository.CurrencyRepository;
import com.g9latam.team62.fintech_api.repository.TransactionRepository;
import com.g9latam.team62.fintech_api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
