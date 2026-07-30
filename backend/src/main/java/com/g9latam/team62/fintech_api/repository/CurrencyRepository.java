package com.g9latam.team62.fintech_api.repository;

import com.g9latam.team62.fintech_api.model.Currency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CurrencyRepository extends JpaRepository<Currency, Long> {

    Optional<Currency> findByNameCurrencyIgnoreCase(String nameCurrency);
}
