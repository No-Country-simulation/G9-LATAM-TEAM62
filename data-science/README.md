# Finance AI — Data Science

Todo el trabajo de Ciencia de Datos del proyecto, organizado por etapa. Cada carpeta es independiente y se puede correr por separado con datos propios de prueba — no dependen de que el backend esté corriendo.

---

## 00_dataset_simulado/

Genera un dataset sintético (2,200 usuarios, ~105,000 transacciones) para pruebas y entrenamiento, sin depender de datos reales.

```bash
python3 generar_dataset.py
```

## 01_analisis_cartolas/

- `analisis_cartola.ipynb` — notebook de exploración: lee cartolas reales (`.xlsx`, `.xls`, `.csv`, `.pdf`) de múltiples bancos chilenos, las limpia, normaliza y clasifica.
- `procesar_cartola_cli.py` — el mismo pipeline, como script de línea de comandos (es el que Backend invoca en producción desde `StatementIngestionService.java`).

```bash
python3 procesar_cartola_cli.py "ruta/a/una/cartola.xlsx"
```

## 02_motor_recomendaciones/

Compara el gasto real de un usuario por categoría contra un presupuesto de referencia (datos del INE, no inventados) y genera alertas. Versión en Python de `BudgetRecommendationService.java` — mismo comportamiento, para probar la lógica sin Java.

```bash
python3 budget_recommendation_engine.py
```

## 03_modelo_clasificacion/

El modelo de Machine Learning (Nivel 3 del pipeline de clasificación) — el único entregable obligatorio del brief que dependía de esto.

- `01_construir_dataset.py` — arma el dataset de entrenamiento combinando los mapeos/palabras clave reales de Backend + datos propios
- `02_entrenar_evaluar.py` — entrena, evalúa, serializa el modelo
- `servicio_inferencia.py` — expone el modelo como API (`POST /predict`)
- `explicabilidad_resumen.py` — arma el resumen de "cómo se clasificaron tus transacciones"
- `reporte-evaluacion-modelo.md` — métricas completas, matriz de confusión, y una advertencia honesta sobre qué mide (y qué no) el accuracy reportado

```bash
pip install -r requirements.txt
python3 01_construir_dataset.py
python3 02_entrenar_evaluar.py
uvicorn servicio_inferencia:app --reload
```

**Nota:** la copia que Backend efectivamente despliega vive en `/ml-model` (raíz del repositorio) — es la misma, duplicada acá para que quede todo el trabajo de Data Science junto en un solo lugar.

## 04_evolutivo_cliente/

Compara el gasto del mes actual de un usuario contra su **propio promedio histórico** (no contra el % fijo del INE) — estadística simple, sin ML. Validado con datos dummy con ruido controlado, a la espera de que se acumule suficiente historial real.

- `01_generar_dummy.py` — genera datos de prueba con ruido realista + anomalías inyectadas
- `02_detectar_desviaciones.py` — el algoritmo, con los parámetros ya ajustados
- `validacion-algoritmo-evolutivo.md` — el proceso completo de validación, incluyendo el primer intento que falló y cómo se corrigió

```bash
python3 01_generar_dummy.py
python3 02_detectar_desviaciones.py
```

**Puerto a Java:** `ClientEvolutionService.java` (en `/backend`), endpoint `POST /api/recommendations/generate-evolution`.

---

## Cómo probar con datos propios

Cada script está pensado para poder correr con datos distintos a los de ejemplo — reemplazá el archivo de entrada (`.csv`, cartola, etc.) por el propio y volvé a correr. Ningún script requiere que el backend esté levantado.
