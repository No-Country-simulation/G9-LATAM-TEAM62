package com.g9latam.team62.fintech_api.service;

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

    public TransactionService(TransactionRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    public Transaction create(Transaction transaction) {
        requireUserExists(transaction.getUserId());
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
            throw new IllegalArgumentException("user " + userId + " does not exist");
        }
    }
}
