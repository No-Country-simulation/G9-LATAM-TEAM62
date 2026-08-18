package com.g9latam.team62.fintech_api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.g9latam.team62.fintech_api.model.Currency;

/**
 * Referencia a una moneda ya registrada. El cliente manda {@code {"id": 1}} o
 * {@code {"name_currency": "CLP"}}; nunca una fila nueva, porque el catálogo de monedas no se
 * amplía desde la API.
 */
public record CurrencyRef(Long id, @JsonProperty("name_currency") String nameCurrency) {

    /** Moneda por omisión cuando la petición no nombra ninguna. */
    public static final CurrencyRef DEFAULT = new CurrencyRef(null, "CLP");

    public Currency toReference() {
        Currency currency = new Currency();
        currency.setId(id);
        currency.setNameCurrency(nameCurrency);
        return currency;
    }
}
