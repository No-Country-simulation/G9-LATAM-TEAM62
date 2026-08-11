package com.g9latam.team62.fintech_api.service;

import java.text.Normalizer;
import java.util.regex.Pattern;
/**
 * Normaliza descripciones de cartolas bancarias para búsquedas exactas y por palabras clave.
 *
 * Pasos:
 * 1. Convertir a mayúsculas.
 * 2. Eliminar tildes y diacríticos.
 * 3. Remover números, fechas, RUTs y códigos de operación.
 * 4. Conservar solo letras y espacios.
 * 5. Colapsar espacios repetidos y aplicar trim.
 *
 * Ejemplos:
 * - "COMPRA JUMBO PROVIDENCIA 1234" -> "COMPRA JUMBO PROVIDENCIA"
 * - "Pago UBER Trip 12345"          -> "PAGO UBER TRIP"
 * - "TEF A CTA 12.345.678-9"        -> "TEF A CTA"
 */
public final class TextNormalizer {

    /** Coincide con marcas diacríticas Unicode (acentos, tildes) tras descomposición NFD. */
    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    /** Coincide con secuencias de dígitos, RUTs o fechas. */
    private static final Pattern NUMBERS_AND_CODES = Pattern.compile("[0-9]+([.\\-/][0-9kK]+)*");

    /** Coincide con cualquier carácter que no sea letra o espacio en blanco. */
    private static final Pattern NON_ALPHA = Pattern.compile("[^A-Z\\s]");

    /** Coincide con dos o más espacios en blanco consecutivos. */
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s{2,}");

    private TextNormalizer() {
        // Clase de utilidad — no instanciable
    }

    /**
     * Normaliza la descripción de una transacción bancaria a su forma canónica.
     *
     * @param rawDescription la descripción original; puede ser {@code null}
     * @return el texto normalizado, o una cadena vacía si la entrada es {@code null}/vacía
     */
    public static String normalize(String rawDescription) {
        if (rawDescription == null || rawDescription.isBlank()) {
            return "";
        }

        // 1. Mayúsculas
        String result = rawDescription.toUpperCase();

        // 2. Descomponer Unicode y eliminar diacríticos (á → A, ñ → N)
        result = Normalizer.normalize(result, Normalizer.Form.NFD);
        result = DIACRITICS.matcher(result).replaceAll("");

        // 3. Remover números, RUTs, fechas y códigos
        result = NUMBERS_AND_CODES.matcher(result).replaceAll("");

        // 4. Remover caracteres no alfabéticos restantes
        result = NON_ALPHA.matcher(result).replaceAll(" ");

        // 5. Colapsar espacios y aplicar trim
        result = MULTI_SPACE.matcher(result).replaceAll(" ").trim();

        return result;
    }
}
