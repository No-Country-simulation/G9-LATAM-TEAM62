package com.g9latam.team62.fintech_api.model;

public enum SavingFrequency {
    NEVER,
    RARELY,
    MONTHLY,
    BIWEEKLY,
    WEEKLY,
    DAILY;

    /**
     * Traduce lo que escribe la interfaz ("Mensualmente", "Ocasionalmente", "Alta"…) al valor del
     * enum. Vive aquí y no en el controlador porque es vocabulario del dominio y se puede probar
     * sin levantar el contexto web: el caso de "Ocasionalmente" —que durante un tiempo se guardaba
     * como {@link #MONTHLY}, justo lo contrario de lo que el usuario decía— solo se detectaba con
     * un test de endpoint.
     *
     * @return la frecuencia reconocida, o {@link #MONTHLY} cuando el texto no dice nada útil
     */
    public static SavingFrequency fromText(String text) {
        if (text == null || text.isBlank()) {
            return MONTHLY;
        }
        String clean = text.trim().toUpperCase();
        if (clean.contains("ALTA") || clean.contains("HIGH") || clean.contains("SEMANAL")) {
            return WEEKLY;
        }
        if (clean.contains("MEDIA") || clean.contains("MEDIUM") || clean.contains("MENSUAL")) {
            return MONTHLY;
        }
        if (clean.contains("BAJA") || clean.contains("LOW") || clean.contains("RARA")
                || clean.contains("OCASIONAL") || clean.contains("A VECES")) {
            return RARELY;
        }
        if (clean.contains("NINGUNA") || clean.contains("NUNCA") || clean.contains("NEVER")) {
            return NEVER;
        }
        try {
            return valueOf(clean);
        } catch (IllegalArgumentException noSuchConstant) {
            return MONTHLY;
        }
    }
}
