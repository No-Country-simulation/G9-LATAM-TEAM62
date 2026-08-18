package com.g9latam.team62.fintech_api.service;

import com.g9latam.team62.fintech_api.dto.ProfileUpdateRequest;
import com.g9latam.team62.fintech_api.exception.ConflictException;
import com.g9latam.team62.fintech_api.exception.NotFoundException;
import com.g9latam.team62.fintech_api.dto.RegisterRequest;
import com.g9latam.team62.fintech_api.dto.UserUpdateRequest;
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
        user.setMonthlyIncome(request.monthlyIncome());
        user.setSavingFrequency(request.savingFrequency());
        
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

    /**
     * Actualiza los datos que el propio usuario administra. El perfil financiero no se toca aquí:
     * lo escribe {@link #updateProfile}, y como la petición ya no puede nombrarlo, tampoco hace
     * falta restaurarlo campo por campo después de recibirlo.
     */
    @Transactional
    public User update(@NonNull Long id, UserUpdateRequest request) {
        User user = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("El usuario " + id + " no existe"));
        requireEmailAvailable(request.email(), id);

        user.setName(request.name());
        user.setEmail(request.email());
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        user.setMonthlyIncome(request.monthlyIncome());
        user.setSavingFrequency(request.savingFrequency());
        return repository.save(user);
    }

    @Transactional
    public User updateProfile(@NonNull Long id, ProfileUpdateRequest request) {
        User user = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("El usuario " + id + " no existe"));
        user.setFinancialProfile(request.financialProfile());
        user.setProfileAccuracy(request.profileAccuracy());
        if (request.savingFrequency() != null) {
            user.setSavingFrequency(request.savingFrequency());
        }
        // Se escribe aquí, sobre la entidad gestionada que este método ya carga y guarda.
        // Hacerlo desde el controlador no funcionaba: allí el User está desligado de la
        // sesión de persistencia y el save() de más abajo lo pisaba con lo que hay en BD.
        if (request.monthlyIncome() != null) {
            user.setMonthlyIncome(request.monthlyIncome());
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
                .orElseThrow(() -> new NotFoundException("No existe un usuario con el correo " + email));
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
                    throw new ConflictException("El correo " + email + " ya está registrado");
                });
    }
}
