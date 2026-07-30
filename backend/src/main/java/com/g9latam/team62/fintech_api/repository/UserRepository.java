package com.g9latam.team62.fintech_api.repository;

import com.g9latam.team62.fintech_api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // emails are matched case-insensitively, so ADA@x.com and ada@x.com are the
    // same login; the unique index on lower(email) enforces it in the database
    @Query("select u from User u where lower(u.email) = lower(:email)")
    Optional<User> findByEmail(@Param("email") String email);
}
