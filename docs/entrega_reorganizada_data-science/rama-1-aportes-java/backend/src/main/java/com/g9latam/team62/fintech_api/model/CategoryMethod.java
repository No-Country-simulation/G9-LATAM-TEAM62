package com.g9latam.team62.fintech_api.model;

// Cómo se determinó la categoría de esta transacción. Independiente de
// TransactionSource: una transacción BANK puede haber sido categorizada por
// mapeo, regla o modelo, y luego corregida por el usuario; una transacción
// MANUAL siempre nace como USER_PROVIDED.
public enum CategoryMethod {
    MAPPING,        // coincidencia exacta en transaction_category_mappings
    KEYWORD_RULE,   // coincidencia por palabra clave (CategoryRule)
    ML_MODEL,       // predicción del modelo entrenado
    FALLBACK,       // ninguno de los anteriores tuvo éxito (OTHER_EXPENSE / OTHER_INCOME)
    USER_PROVIDED,  // el usuario la ingresó directamente (registro manual)
    USER_CORRECTED  // el usuario corrigió una categoría sugerida por cualquiera de los niveles automáticos
}
