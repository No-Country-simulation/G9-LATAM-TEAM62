package com.g9latam.team62.fintech_api.repository;

import com.g9latam.team62.fintech_api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
