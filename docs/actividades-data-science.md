# Finance AI — Actividades de Data Science

**Para:** el equipo completo (backend, frontend, coordinación)
**De:** área de Ciencia de Datos / Arquitectura
**Propósito:** registro de todo lo trabajado en esta área hasta la fecha — qué se hizo, cómo, y qué queda pendiente.

---

## 1. Dataset simulado

**Archivo:** `data-science/generar_dataset.py`

Generador reproducible (semilla fija = 42) de un dataset sintético para entrenar el futuro clasificador:
- **2,200 usuarios** con ingreso mensual, nivel de endeudamiento y frecuencia de ahorro
- **~105,000 transacciones** repartidas en 3 meses, con descripciones de comercios realistas por categoría
- **Perfil financiero** calculado con una fórmula que combina endeudamiento, ahorro y ratio gasto/ingreso, más ruido controlado — a propósito no es una regla perfecta, para que un futuro modelo tenga algo real que aprender

---

## 2. Análisis de cartolas bancarias reales

**Archivo:** `data-science/analisis_cartola.ipynb`

Se analizaron cartolas reales de 4 fuentes (Banco Chile, CuentaRUT, Banco Falabella, Mercado Pago) para validar supuestos contra datos reales, no solo simulados. Hallazgos clave:

- Ningún banco entrega categoría — confirma que la clasificación por texto es indispensable, no un atajo
- El monto casi siempre llega separado en Cargo/Abono, no como un valor único con signo
- Las descripciones reales son más "ruidosas" que cualquier dato simulado (prefijos como `PAGO`, `TEF DE/PARA`, códigos de sucursal)

**El notebook construido a partir de esto:**
- Lee `.xlsx`, `.xls`, `.csv` y `.pdf`, detectando automáticamente banco, país, año y fila de encabezado
- Normaliza montos, fechas en español, y texto (mayúsculas, sin tildes, sin números variables)
- Clasifica por palabras clave usando **las 13 categorías reales de `Category.java`** del backend — se actualizó a propósito para no inventar categorías propias (ver sección 6)
- Probado de punta a punta contra las 4 cartolas reales, sin errores

---

## 3. Plan de clasificación híbrido

**Archivos:** `docs/plan-clasificacion-equipo-datos.md`, `docs/plan-clasificacion-unificado.md`, `docs/plan-clasificacion-comparacion.md`

Un integrante de Backend propuso, en paralelo y de forma independiente, un sistema de reglas + mapeo aprendido en base de datos (sin ML). Se comparó contra la propuesta original de Ciencia de Datos (modelo ML entrenado) y se unificaron en una arquitectura de **4 niveles**, de mayor a menor precisión:

```
1. Mapeo exacto en BD               → Backend
2. Reglas de palabras clave         → Backend
3. Modelo ML entrenado              → Datos
4. Fallback (OTHER_EXPENSE/INCOME)  → ambos
```

**Estado:** diseñado y documentado, coincide con el 100% de las categorías reales del sistema. **No implementado en código todavía** (ni la parte de Backend — mapeo/reglas — ni la de Datos — el modelo entrenado).

---

## 4. Motor de Recomendaciones Presupuestarias

**Archivos:** `data-science/budget_recommendation_engine.py` (Python) y `backend-aporte/service/BudgetRecommendationService.java` (Java) — mismo comportamiento verificado en ambos.

Compara cómo un usuario reparte su gasto entre categorías contra un **porcentaje de referencia**, y genera recomendaciones cuando hay desbalance. **Deliberadamente sin Machine Learning** — es aritmética sobre datos ya categorizados:

- 3 niveles de severidad según cuánto se excede el % recomendado (1.2x / 1.5x / 2x)
- Chequeo de tasa de ahorro (objetivo: 20%), aparte del análisis por categoría
- Máximo 3 recomendaciones por corrida + enfriamiento de 7 días, para no saturar al usuario
- Umbral mínimo de datos (5 transacciones) antes de generar cualquier recomendación

**Los porcentajes de referencia (Chile):** salen de la **IX Encuesta de Presupuestos Familiares del INE (2022-2023)**, no son inventados. Tabla `category_budget_targets`, con columna de país para escalar a otros mercados más adelante insertando filas nuevas — sigue siendo el elemento comparativo del análisis, tal como se definió.

**Transferencias:** se detectan por texto en la descripción (`TRANSF`, `TEF`, `GIRO`) y se excluyen del cálculo de % de gasto — el dinero queda igual reflejado en la base de datos, solo no cuenta como gasto de consumo. Esto se decidió así a propósito para no tener que agregar una categoría nueva al `Category.java` real del backend (ver sección 6).

**Estado:** construido, probado, listo para integrar — **todavía no mergeado** al repositorio real del equipo.

---

## 5. Tabla de historial de perfil financiero

**Archivos:** `backend-aporte/db/003_financial_profile_history.sql`, `FinancialProfileHistory.java` + repositorio, hook en `UserService.updateProfile()`.

`users.financial_profile` se sobreescribe en cada actualización — no se podía reconstruir la evolución del perfil financiero de un usuario en el tiempo. Se agregó una tabla de historial aparte, enlazada por FK a `users`, que guarda una fila cada vez que se actualiza el perfil.

**Esta tabla es la base necesaria para el "análisis evolutivo del cliente"** (ver sección 7) — sin historial acumulado no hay evolución que analizar.

**Estado:** diseñado y construido en esta iteración, **todavía no mergeado**.

---

## 6. Decisiones de alcance que vale la pena que el equipo conozca

- **No se agregó ninguna categoría ni tipo nuevo al `Category.java`/`TransactionType.java` real** — se decidió adaptar nuestro trabajo al backend tal como está, no pedirles cambios a ellos. Las transferencias se resuelven por detección de texto, no por una categoría "transferencia".
- **Tarjeta de crédito excluida del MVP a propósito** — el ciclo de facturación es distinto al de la cuenta corriente y complica la conciliación cartola↔registro manual; queda para una iteración futura.
- **Registro manual de transacciones** (`POST /api/transactions/manual`) y **corrección de categoría** (`PUT /api/transactions/{id}/category`) — diseñados y construidos, con lógica de conciliación (mismo monto, ventana de 2-3 días) documentada mecánicamente, pero **el job de conciliación en sí todavía no está implementado**, solo el modelo de datos que lo soporta (`link_status`, `linked_transaction_id`).

---

## 7. Próximos pasos (en orden sugerido)

1. **Entrenar el modelo de clasificación (nivel 3 del plan híbrido)** — es el único entregable obligatorio del brief que sigue sin construirse. El dataset simulado ya está listo para esto.
2. **Mergear al repo real:** el motor de recomendaciones presupuestarias y la tabla de historial de perfil — ambos ya construidos y probados, solo falta la integración.
3. **Implementar el "análisis evolutivo del cliente"** — comparar el % de gasto actual de un usuario contra su propio promedio histórico (media y desviación estándar simples, no ML), usando `financial_profile_history` como fuente. Es el paso intermedio entre BI y ML que se había definido: mejora la personalización sin necesitar entrenar nada nuevo, y sienta las bases para un futuro modelo de comportamiento cuando haya suficientes datos reales acumulados.
4. **Job de conciliación cartola↔registro manual** — el modelo de datos ya lo soporta, falta el proceso batch que lo ejecute.
5. **Reconciliar la clasificación mockeada del frontend** — el `AnalysisContext.tsx` actual usa 5 categorías propias en español rioplatense, desconectadas de las 13 categorías reales y de nuestro trabajo. Cuando el frontend conecte con la API real, esa lógica mock debería reemplazarse por llamadas reales.

---

## 8. Script de ingesta invocable + contrato para Backend

**Archivos:** `data-science/procesar_cartola_cli.py`, `docs/contrato-ingesta-cartolas.md`

Se decidió, en conjunto con Backend, **separar responsabilidades**: en vez de que Ciencia de Datos escriba el endpoint Java que integra la lectura de cartolas, se entrega el script y un contrato de integración explícito, para que Backend construya su propio endpoint encima — sin que dos equipos escriban lógica de negocio duplicada, y sin que Ciencia de Datos tenga que mantener código Java.

`analisis_cartola.ipynb` se convirtió en `procesar_cartola_cli.py`: mismo pipeline exacto, ahora invocable por línea de comandos (`python3 procesar_cartola_cli.py <archivo>`), con salida en un único JSON por stdout y códigos de salida estándar (`0` éxito, `1` error). Probado contra las 4 cartolas reales — funciona igual que el notebook, sin necesitar Jupyter.

**Por qué este camino y no un servicio Python aparte:** se evaluaron 3 alternativas (portar todo a Java, servicio Python independiente escribiendo directo a Oracle, o Java invocando Python como subproceso). Se descartó el servicio aparte por 3 riesgos concretos: lógica de negocio duplicada entre Java y Python, credenciales de Oracle duplicadas, y que el frontend nunca recibiría respuesta (no hay ciclo de petición/respuesta hacia el usuario). El detalle completo de la decisión queda en la conversación del proyecto, no en un documento aparte — este resumen es suficiente para entender el "por qué" del contrato.

**Estado:** script entregado y probado; **el endpoint, el `Dockerfile`, y la escritura en base de datos quedan como trabajo de Backend**, con el contrato ya cerrado (sección 6 del inventario, `docs/contrato-ingesta-cartolas.md`).

---

*Documento vivo — actualizar a medida que estas piezas se integren al repositorio real.*
