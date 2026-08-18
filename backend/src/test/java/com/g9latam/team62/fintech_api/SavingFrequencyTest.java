package com.g9latam.team62.fintech_api;

import com.g9latam.team62.fintech_api.model.SavingFrequency;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El vocabulario que escribe la interfaz vive en el enum, así que se puede probar sin levantar el
 * contexto: antes esta traducción estaba dentro del controlador y solo se ejercitaba por HTTP.
 */
class SavingFrequencyTest {

    @Test
    void reconoceLasOpcionesQueOfreceLaInterfaz() {
        assertThat(SavingFrequency.fromText("Semanalmente")).isEqualTo(SavingFrequency.WEEKLY);
        assertThat(SavingFrequency.fromText("Mensualmente")).isEqualTo(SavingFrequency.MONTHLY);
        assertThat(SavingFrequency.fromText("Ocasionalmente")).isEqualTo(SavingFrequency.RARELY);
        assertThat(SavingFrequency.fromText("Nunca")).isEqualTo(SavingFrequency.NEVER);
    }

    @Test
    void aceptaElNombreDelEnumTalCual() {
        assertThat(SavingFrequency.fromText("biweekly")).isEqualTo(SavingFrequency.BIWEEKLY);
    }

    @Test
    void caeEnMensualCuandoElTextoNoDiceNada() {
        assertThat(SavingFrequency.fromText(null)).isEqualTo(SavingFrequency.MONTHLY);
        assertThat(SavingFrequency.fromText("  ")).isEqualTo(SavingFrequency.MONTHLY);
        assertThat(SavingFrequency.fromText("cuando se pueda")).isEqualTo(SavingFrequency.MONTHLY);
    }
}
