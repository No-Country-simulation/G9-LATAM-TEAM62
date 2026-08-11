package com.g9latam.team62.fintech_api.service;

import com.g9latam.team62.fintech_api.dto.ProfileUpdateRequest;
import com.g9latam.team62.fintech_api.model.User;
import com.g9latam.team62.fintech_api.repository.RecommendationRepository;
import com.g9latam.team62.fintech_api.repository.TransactionRepository;
import com.g9latam.team62.fintech_api.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import org.springframework.lang.NonNull;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository repository;
    private final TransactionRepository transactionRepository;
    private final RecommendationRepository recommendationRepository;
    private final com.g9latam.team62.fintech_api.repository.FinancialProfileHistoryRepository historyRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repository, TransactionRepository transactionRepository,
                       RecommendationRepository recommendationRepository,
                       com.g9latam.team62.fintech_api.repository.FinancialProfileHistoryRepository historyRepository,
                       PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.transactionRepository = transactionRepository;
        this.recommendationRepository = recommendationRepository;
        this.historyRepository = historyRepository;
        this.passwordEncoder = passwordEncoder;
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

    public Optional<User> findById(@NonNull Long id) {
        return repository.findById(id);
    }

    @Transactional
    public User update(@NonNull Long id, User user) {
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
    public User updateProfile(@NonNull Long id, ProfileUpdateRequest request) {
        User user = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("user " + id + " does not exist"));
        user.setFinancialProfile(request.financialProfile());
        user.setProfileAccuracy(request.profileAccuracy());
        if (request.savingFrequency() != null) {
            user.setSavingFrequency(request.savingFrequency());
        }
        user.setProfileUpdatedAt(LocalDateTime.now());

        // Registra la entrada en el historial de evolución del perfil financiero
        historyRepository.save(new com.g9latam.team62.fintech_api.model.FinancialProfileHistory(
                null, id, request.financialProfile(), request.profileAccuracy(), LocalDateTime.now()
        ));

        return repository.save(user);
    }

    @Transactional
    public Optional<User> authenticate(String email, String rawPassword) {
        Optional<User> userOpt = repository.findByEmail(email);
        if (userOpt.isEmpty()) {
            User fallbackUser = new User();
            String name = "Usuario";
            if (email != null && email.contains("@")) {
                String localPart = email.substring(0, email.indexOf("@"));
                if (!localPart.isBlank()) {
                    name = java.util.Arrays.stream(localPart.split("[._-]"))
                            .filter(s -> !s.isBlank())
                            .map(s -> Character.toUpperCase(s.charAt(0)) + (s.length() > 1 ? s.substring(1).toLowerCase() : ""))
                            .collect(java.util.stream.Collectors.joining(" "));
                    if (name.isBlank()) {
                        name = "Usuario";
                    }
                }
            }
            fallbackUser.setName(name);
            fallbackUser.setEmail(email);
            fallbackUser.setPassword(rawPassword);
            fallbackUser.setMonthlyIncome(java.math.BigDecimal.ZERO);
            fallbackUser.setSavingFrequency(com.g9latam.team62.fintech_api.model.SavingFrequency.MONTHLY);
            return Optional.of(create(fallbackUser));
        }
        return userOpt.filter(user -> passwordEncoder.matches(rawPassword, user.getPassword()));
    }

    @Transactional
    public void changePassword(String email, String oldPassword, String newPassword) {
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Incorrect password");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        repository.save(user);
    }

    // one transaction: children first, so the foreign keys never dangle
    @Transactional
    public void delete(@NonNull Long id) {
        transactionRepository.deleteByUserId(id);
        recommendationRepository.deleteByUserId(id);
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
