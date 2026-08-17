package com.g9latam.team62.fintech_api.service;

import com.g9latam.team62.fintech_api.dto.ProfileUpdateRequest;
import com.g9latam.team62.fintech_api.dto.RegisterRequest;
import com.g9latam.team62.fintech_api.model.FinancialProfileHistory;
import com.g9latam.team62.fintech_api.model.User;
import com.g9latam.team62.fintech_api.repository.FinancialProfileHistoryRepository;
import com.g9latam.team62.fintech_api.repository.RecommendationRepository;
import com.g9latam.team62.fintech_api.repository.TransactionRepository;
import com.g9latam.team62.fintech_api.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.lang.NonNull;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final LoginAttemptService loginAttemptService;
    private final UserRepository repository;
    private final TransactionRepository transactionRepository;
    private final RecommendationRepository recommendationRepository;
    private final FinancialProfileHistoryRepository historyRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repository, TransactionRepository transactionRepository,
                       RecommendationRepository recommendationRepository,
                       FinancialProfileHistoryRepository historyRepository,
                       PasswordEncoder passwordEncoder, LoginAttemptService loginAttemptService) {
        this.repository = repository;
        this.transactionRepository = transactionRepository;
        this.recommendationRepository = recommendationRepository;
        this.historyRepository = historyRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
    }

    @Transactional
    public User create(RegisterRequest request) {
        requireEmailAvailable(request.email(), null);
        
        // Creamos una entidad limpia desde cero
        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        
        // No necesitamos setear ID ni perfiles a null, ¡porque ya son null al nacer!
        return repository.save(user);
    }

    public Collection<User> findAll() {
        return repository.findAll();
    }

    public Optional<User> findById(@NonNull Long id) {
        return repository.findById(id);
    }

    public Optional<User> findByEmail(@NonNull String email) {
        return repository.findByEmail(email);
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
        historyRepository.save(new FinancialProfileHistory(
                null, id, request.financialProfile(), request.profileAccuracy(), LocalDateTime.now()
        ));

        return repository.save(user);
    }

    public User authenticate(String email, String rawPassword) {
        if (loginAttemptService.isBlocked(email)) {
            throw new IllegalStateException("Cuenta bloqueada temporalmente por múltiples intentos fallidos. Intenta en 15 minutos.");
        }

        User user = repository.findByEmail(email)
                .orElseThrow(() -> {
                    loginAttemptService.loginFailed(email);
                    return new IllegalArgumentException("Credenciales inválidas");
                });

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            loginAttemptService.loginFailed(email);
            throw new IllegalArgumentException("Credenciales inválidas");
        }

        loginAttemptService.loginSucceeded(email);
        return user;
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

    public List<FinancialProfileHistory> findProfileHistory(Long userId) {
        return historyRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    private void requireEmailAvailable(String email, Long ownId) {
        repository.findByEmail(email)
                .filter(other -> !other.getId().equals(ownId))
                .ifPresent(other -> {
                    throw new IllegalStateException("email " + email + " is already registered");
                });
    }
}
