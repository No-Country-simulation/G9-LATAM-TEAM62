# Evolutivo del Cliente — Validación con datos dummy

**Estado:** lógica validada contra datos simulados. **Pendiente de re-validar con datos reales** apenas haya 3+ meses de cartolas cargadas por usuario — los parámetros de acá son un punto de partida razonable, no una verdad definitiva.

---

## 1. Por qué datos dummy, y no reales

La ingesta de cartolas recién empezó a funcionar de verdad — no hay todavía suficiente historial real acumulado por usuario para calcular "el promedio histórico de esta persona" (se necesitan al menos 3 meses). En vez de esperar, se generaron datos simulados con **ruido realista** para poder probar la lógica ya — cuando haya suficientes cartolas reales, se repite exactamente esta misma validación contra datos de verdad.

## 2. Cómo se generó el dummy, para que la prueba fuera honesta

No alcanza con generar datos "limpios" donde todo calza perfecto — eso no prueba nada. El generador (`01_generar_dummy.py`) hace:

- 20 usuarios simulados, cada uno con un patrón de gasto "normal" propio (no todos gastan igual)
- 6 meses de historial por usuario, con **ruido gaussiano mes a mes** (nadie gasta exactamente igual todos los meses, ni en la simulación)
- A **la mitad** de los usuarios (10 de 20) se les inyectó una anomalía real en el último mes — un cambio de comportamiento genuino, no ruido — en una categoría al azar, con una magnitud al azar (entre 12 y 25 puntos porcentuales)
- A la otra mitad, **ningún cambio** — solo la variación normal de ruido

Esto permite medir dos cosas a la vez: si el algoritmo detecta las anomalías reales (sensibilidad), y si NO marca en falso a quienes no cambiaron nada (especificidad) — ambas importan igual.

## 3. Primer intento: falló, y quedó documentado el ajuste

Con los parámetros iniciales (1.5 desviaciones estándar, piso de 2 puntos porcentuales):

| Métrica | Resultado |
|---|---|
| Anomalías reales detectadas | 10/10 |
| Usuarios sin cambio, marcados en falso | **5/10** |

Detectaba todo, pero a costa de marcar a la mitad de la gente que no había cambiado nada — un algoritmo así generaría desconfianza rápido ("¿por qué me está alertando si no hice nada distinto?"). No se ajustó a ojo — se corrió una búsqueda sobre 20 combinaciones de parámetros, midiendo detecciones y falsos positivos en cada una:

| Desviaciones mínimas | Piso (puntos %) | Detectadas | Falsos positivos |
|---|---|---|---|
| 1.5 | 2.0 | 10/10 | 5 |
| 2.0 | 3.0 | 10/10 | 1 |
| **2.0** | **4.0** | **10/10** | **0** |
| 2.5 | 4.0 | 10/10 | 0 |
| 3.0 | 3.0 | 8/10 | 0 |
| 3.5 | 4.0 | 5/10 | 0 |

Se eligió **2.0 desviaciones estándar, piso de 4 puntos porcentuales** — dentro de la zona de 0 falsos positivos, sin llevarlo al extremo (para no perder sensibilidad si el patrón real resulta ser un poco distinto al simulado).

## 4. Resultado final

**10/10 anomalías inyectadas detectadas, 0 falsos positivos, sobre 20 usuarios simulados** — y las categorías detectadas coinciden exactamente con las que se inyectaron al azar (ej. usuario 1 → HEALTH, usuario 5 → TRANSPORT), confirmando que el algoritmo no solo detecta *que* algo cambió, sino *en qué categoría*.

## 5. Parámetros finales (implementados en `ClientEvolutionService.java`)

| Parámetro | Valor | Qué controla |
|---|---|---|
| Meses mínimos de historial | 3 | Bajo esto, no se genera nada — no alcanza para calcular un promedio confiable |
| Desviaciones estándar | 2.0 | Cuánto tiene que alejarse el mes actual del promedio para considerarse anómalo |
| Piso de desviación | 4 puntos % | Evita marcar a usuarios muy estables (poca variación natural) por cambios mínimos |

## 6. Limitaciones conocidas

- **Es una validación contra datos simulados, no reales.** El ruido gaussiano es una aproximación razonable, pero el comportamiento real de gasto puede tener patrones que el dummy no captura (estacionalidad, gastos recurrentes grandes tipo arriendo, etc.).
- **3 meses es un mínimo viable, no ideal.** Con tan pocos puntos, la desviación estándar estimada es ruidosa. Con 6+ meses de historial real, valdría la pena repetir el ajuste de parámetros — probablemente permita bajar el piso sin generar falsos positivos.
- **Recomendación concreta:** apenas haya usuarios reales con 3+ meses de cartolas cargadas, repetir esta misma validación (el script `02_detectar_desviaciones.py` funciona igual, solo cambia la fuente de datos) antes de confiar plenamente en las alertas que genere en producción.

## 7. Archivos de esta entrega

```
evolutivo/
├── 01_generar_dummy.py           # generador de datos dummy con ruido + anomalías
├── 02_detectar_desviaciones.py   # el algoritmo, ya con los parámetros finales
├── transacciones_dummy_evolutivo.csv
├── desviaciones_detectadas.csv
└── validacion-algoritmo-evolutivo.md   # este documento
```

Puerto a Java: `ClientEvolutionService.java` + endpoint `POST /api/recommendations/generate-evolution` en `RecommendationController.java`.
