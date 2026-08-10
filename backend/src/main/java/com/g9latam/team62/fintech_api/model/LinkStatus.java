package com.g9latam.team62.fintech_api.model;

// Estado de conciliación de una transacción MANUAL contra una transacción
// BANK real. El job de conciliación (a implementar cuando haya cartolas
// cargadas) es quien mueve esto de UNLINKED a AUTOMATIC o USER_CONFIRMED.
public enum LinkStatus {
    UNLINKED,
    AUTOMATIC,
    USER_CONFIRMED
}
