package com.g9latam.team62.fintech_api.service;

import com.g9latam.team62.fintech_api.dto.ProfileUpdateRequest;
import com.g9latam.team62.fintech_api.model.FinancialProfileHistory;
import com.g9latam.team62.fintech_api.model.User;
import com.g9latam.team62.fintech_api.repository.FinancialProfileHistoryRepository;
import com.g9latam.team62.fintech_api.repository.RecommendationRepository;
import com.g9latam.team62.fintech_api.repository.TransactionRepository;
import com.g9latam.team62.fintech_api.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository repository;
    private final TransactionRepository transactionRepository;
    private final RecommendationRepository recommendationRepository;
    // -- aporte --
    private final FinancialProfileHistoryRepository profileHistoryRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository repository, TransactionRepository transactionRepository,
                       RecommendationRepository recommendationRepository,
                       FinancialProfileHistoryRepository profileHistoryRepository) {
        this.repository = repository;
        this.transactionRepository = transactionRepository;
        this.recommendationRepository = recommendationRepository;
        this.profileHistoryRepository = profileHistoryRepository;
    }

    @Transactional
    public User create(User user) {
        requireEmailAvailable(user.getEmail(), null);
        user.setId(null);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        // profile fields are only written through updateProfile
        user.setFinancialProfile(null);
        user.setProfileAccuracy(null);
        user.setProfileUpdatedAt(null);
        return repository.save(user);
    }

    public Collection<User> findAll() {
        return repository.findAll();
    }

    public Optional<User> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public User update(Long id, User user) {
        User existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("user " + id + " does not exist"));
        requireEmailAvailable(user.getEmail(), id);
        user.setId(id);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setFinancialProfile(existing.getFinancialProfile());
        user.setProfileAccuracy(existing.getProfileAccuracy());
        user.setProfileUpdatedAt(existing.getProfileUpdatedAt());
        return repository.save(user);
    }

    @Transactional
    public User updateProfile(Long id, ProfileUpdateRequest request) {
        User user = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("user " + id + " does not exist"));
        user.setFinancialProfile(request.financialProfile());
        user.setProfileAccuracy(request.profileAccuracy());
        if (request.savingFrequency() != null) {
            user.setSavingFrequency(request.savingFrequency());
        }
        user.setProfileUpdatedAt(LocalDateTime.now());
        User saved = repository.save(user);

        // -- aporte -- una fila más en el historial, nunca se sobreescribe.
        // Es lo único nuevo acá: el resto del método es exactamente el original.
        profileHistoryRepository.save(new FinancialProfileHistory(
                null, saved.getId(), saved.getFinancialProfile(), saved.getProfileAccuracy(), saved.getProfileUpdatedAt()));

        return saved;
    }

    public Optional<User> authenticate(String email, String rawPassword) {
        return repository.findByEmail(email)
                .filter(user -> passwordEncoder.matches(rawPassword, user.getPassword()));
    }

    // one transaction: children first, so the foreign keys never dangle
    @Transactional
    public void delete(Long id) {
        transactionRepository.deleteByUserId(id);
        recommendationRepository.deleteByUserId(id);
        profileHistoryRepository.deleteByUserId(id); // -- aporte --
        repository.deleteById(id);
    }

    private void requireEmailAvailable(String email, Long ownId) {
        repository.findByEmail(email)
                .filter(other -> !other.getId().equals(ownId))
                .ifPresent(other -> {
                    throw new IllegalStateException("email " + email + " is already registered");
                });
    }
}
