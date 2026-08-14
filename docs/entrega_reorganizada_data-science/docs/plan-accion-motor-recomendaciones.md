# Motor de Recomendaciones Presupuestarias — Plan de Acción

**Proyecto:** Finance AI — Hackathon ONE Alura Latam + Oracle
**Nombre formal de la funcionalidad:** *Motor de Recomendaciones Presupuestarias* (unifica lo que el equipo venía llamando informalmente "la propiedad" / "el propertie")
**Estado:** Listo para entregar a Backend — código Java incluido, además de una versión en Python

---

## 1. Qué vamos a hacer

Comparar cómo un usuario reparte su gasto entre categorías contra un **porcentaje de referencia por categoría**, y generar recomendaciones automáticas cuando hay un desbalance — sin Machine Learning, con aritmética simple sobre los datos ya categorizados.

---

## 2. Decisión 1 — Las transferencias tienen su propia categoría

**Actualización:** el equipo definió que nos adaptamos a la versión actual del backend, no al revés — así que esta decisión cambió de forma, aunque el objetivo se mantiene intacto.

Tenías razón: una transferencia sigue siendo un egreso real de dinero desde la cuenta, aunque no sea un "gasto" en el sentido de consumo. Pero en vez de agregar una categoría/tipo nuevo a `Category.java`/`TransactionType.java` (lo que hubiera requerido coordinar un cambio con Backend), la solución quedó así:

- Las transferencias se siguen guardando como transacciones normales, con la categoría que les asigne el clasificador según su dirección (`OTHER_EXPENSE` si es cargo, `OTHER_INCOME` si es abono) — **el dinero sigue completamente reflejado, nada se pierde ni se oculta**.
- El motor de recomendaciones (Java y Python) las **detecta por texto en la descripción** (`TRANSF`, `TEF`, `GIRO`, `TRANSFERENCIA` — mismas palabras clave que ya usaba el notebook) y las excluye del cálculo de % de gasto, sin necesidad de ningún campo ni categoría nueva.

**Cero cambios requeridos en `Category.java` o `TransactionType.java` reales.** Todo el ajuste vive en nuestro propio servicio.

---

## 3. Decisión 2 — Porcentajes de referencia, con datos reales de Chile

Tal como pediste, no inventé los números — los saqué de la **IX Encuesta de Presupuestos Familiares del INE (2022-2023)**, que mide exactamente esto: cómo reparten su gasto los hogares chilenos.

| Categoría INE | % INE | Categoría del sistema | % asignado |
|---|---|---|---|
| Vivienda y servicios básicos | 29% | `HOUSING` + `UTILITIES` | 20% + 9% |
| Alimentación | 19% | `FOOD` | 28% (se suma con "Restaurantes") |
| Restaurantes y comidas fuera | 9% | `FOOD` | *(incluido arriba)* |
| Transporte | 14% | `TRANSPORT` | 14% |
| Salud | 7% | `HEALTH` | 7% |
| Educación | 6% | `EDUCATION` | 6% |
| Vestimenta y calzado | 5% | `SHOPPING` | 5% |
| Recreación y cultura | 5% | `ENTERTAINMENT` | 5% |
| Otros gastos | 6% | `OTHER_EXPENSE` | 6% |

Dos ajustes que tuve que hacer al traducir la EPF a nuestras categorías (quedan documentados para que el equipo los pueda cuestionar):
- El INE junta "vivienda" y "servicios básicos" en un solo 29% — nuestro sistema los separa en `HOUSING`/`UTILITIES`, así que dividí ese 29% en 20%/9% (estimación propia, no viene desglosada así en la fuente).
- El INE separa "alimentación" (supermercado) de "restaurantes fuera de casa" — nuestro `FOOD` no hace esa distinción, así que sumé ambos en 28%.

**Sobre la extensión a otros países:** la tabla queda diseñada con una columna `country_code` desde ahora (por defecto `'CL'`), así que agregar Perú, México, Colombia, etc. más adelante es solo insertar filas nuevas — no hay que tocar el modelo de datos ni el servicio.

---

## 4. Decisión 3 — Qué le faltaba a la lógica original

Tenías razón en que era demasiado simple. Le agregué 4 cosas:

1. **Umbral mínimo de datos**: si un usuario tiene muy pocas transacciones en el período (por defecto, menos de 5) o gasto total $0, no se genera ninguna recomendación — evita conclusiones ruidosas con poca información.
2. **Niveles de severidad**, en vez de un único umbral binario:
   - `MODERADO`: el gasto real es 1.2x–1.5x el recomendado
   - `ALTO`: 1.5x–2x
   - `SEVERO`: más de 2x
3. **Chequeo de tasa de ahorro**, independiente de las categorías: si `(ingresos - gastos) / ingresos` cae bajo un objetivo (20% por defecto), se genera una recomendación aparte sobre ahorro — esto conecta con lo que ya habíamos hablado del perfil financiero.
4. **Máximo de recomendaciones por corrida** (top 3 desbalances más severos) y **enfriamiento de 7 días** por usuario, para no inundarlo con la misma alerta repetida cada vez que se recalcula.

---

## 5. Decisión 4 — Tabla de historial (para la conversación de ML que dejamos pendiente)

Como mencionaste que te gustó la idea del historial simple: dejé la puerta abierta pero **no la until implementé todavía** — el `Recommendation` que ya existe en el backend guarda `generated_at` y `profile_at_generation`, así que ya hay una traza mínima en el tiempo sin agregar nada. Si más adelante quieren pasar a comparar contra el promedio histórico *del propio usuario* (el paso intermedio antes de ML del que hablamos), se puede sumar una tabla `category_spending_snapshots` (usuario, categoría, período, % real) sin romper nada de lo que se entrega hoy. Lo dejamos como siguiente conversación, tal como propusiste.

---

## 6. Ya no hace falta coordinar ningún cambio de schema/enum con Backend

Con el enfoque de detección por descripción (sección 2), este motor corre tal cual contra el backend real, sin pedirles ningún cambio. Lo único que se entrega es aditivo y autocontenido: la tabla `category_budget_targets` y el servicio — ninguno de los dos modifica `Transaction`, `User`, `Category` ni `TransactionType`.

## 6bis. Notebook actualizado — mismas categorías que Backend

Se actualizó `analisis_cartola.ipynb` para que la columna `CATEGORIA` que produce use **exactamente** las 13 categorías de `Category.java` (antes usaba su propio set: `FOOD, TRANSPORT, HEALTH, ENTERTAINMENT, SERVICES, TRANSACTIONS, Otros`, que no calzaba). Cambios concretos:

- `SERVICES` → `UTILITIES` (y se sumaron utilities reales que faltaban: `ENEL`, `AGUAS ANDINAS`, `VTR`, `GASCO`)
- Se agregaron `HOUSING`, `EDUCATION`, `SHOPPING`, `SALARY` (categorías que el notebook no cubría antes)
- La categorización ahora usa la **dirección del movimiento** (`abono`/`cargo`) para elegir el diccionario correcto y el fallback — antes un abono mal descrito podía terminar en una categoría de gasto, lo cual rompía la semántica de `TransactionType`
- El fallback ya no es `"Otros"` (categoría inexistente en el backend) — es `OTHER_EXPENSE` u `OTHER_INCOME`, según dirección
- Probado de punta a punta contra las 4 cartolas reales (Banco Chile, CuentaRUT, Falabella, Mercado Pago) — corre sin errores y cada fila queda con una categoría válida

---

## 7. Qué se entrega hoy

- `category_budget_targets.sql` — tabla nueva, independiente, con los 9 porcentajes de Chile cargados y columna `country_code` para escalar a otros países
- `BudgetRecommendationService.java` + entidad/repositorio de soporte — listo para Backend
- `budget_recommendation_engine.py` — misma lógica, para Ciencia de Datos / Docker / quien quiera probarla fuera de Java

---

## 8. Si el equipo no sigue con esto

Es perfectamente viable que lo sigamos nosotros. El diseño es intencionalmente independiente: vive en su propia tabla, su propio servicio, y solo depende de `Category`/`TransactionType` (que de todos modos hay que tocar una vez por lo de `TRANSACTIONS`). No modifica `User` ni la lógica de perfil financiero existente, así que no hay riesgo de pisarnos con lo que Backend está construyendo en paralelo.
