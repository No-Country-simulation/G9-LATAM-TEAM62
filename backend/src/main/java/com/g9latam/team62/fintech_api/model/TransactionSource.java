package com.g9latam.team62.fintech_api.model;

// De dónde vino la transacción: BANK = leída desde una cartola cargada por el
// usuario y clasificada por el modelo; MANUAL = registrada a mano por el
// usuario en el momento de la compra.
public enum TransactionSource {
    BANK,
    MANUAL
}
