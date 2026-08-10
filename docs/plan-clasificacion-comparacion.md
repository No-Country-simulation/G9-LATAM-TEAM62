# Plan de Clasificación — Comparación y Resultado de la Unificación

**Proyecto:** Finance AI — Hackathon ONE Alura Latam + Oracle
**Referencias:** `plan-clasificacion-equipo-datos.md` (propuesta Datos) · propuesta original de Backend (`Plan de clasificación.md`) · `plan-clasificacion-unificado.md` (resultado)

---

## 1. Resumen de cada propuesta

**Propuesta de Backend:** sistema jerárquico de 3 niveles sin ML — mapeo aprendido en BD (crowdsourcing), reglas de palabras clave, y fallback a "Otros". Aprende en tiempo real cada vez que un usuario corrige una categoría.

**Propuesta de Datos:** clasificador de Machine Learning supervisado, entrenado offline sobre un dataset propio, con trazabilidad de origen (`categoria_modelo` vs `categoria_usuario`) y reentrenamiento periódico.

---

## 2. Similitudes

| Punto en común | Detalle |
|---|---|
| Normalización de texto | Ambas llegaron, de forma independiente, a la misma necesidad: mayúsculas, sin tildes, sin números/códigos variables antes de comparar o clasificar |
| Categoría "Otros" como fallback | Mismo concepto de última instancia cuando no hay forma de clasificar |
| Feedback humano como mejora continua | El `learnFromFeedback` de Backend y la Alternativa 2 (reclasificación) de Datos son, en esencia, la misma idea implementada en momentos distintos del ciclo |
| Construyen sobre el mismo modelo de datos | Ambas propuestas usan el enum `Category` y la entidad `Transaction` ya existentes en el repositorio real |

---

## 3. Diferencias

| Aspecto | Backend | Datos |
|---|---|---|
| Técnica base | Reglas + tabla de mapeos, sin ML | Modelo supervisado (Scikit-Learn) |
| Cuándo aprende | En tiempo real, cada corrección se escribe al toque | En ciclos de reentrenamiento (S3 y periódicos) |
| Arranque en frío | Funciona desde el día 1 | Depende de que el dataset y el entrenamiento estén listos |
| Confianza de la predicción | Contador de frecuencia (no es una probabilidad real) | Probabilidad calculada por transacción |
| Generalización a texto nuevo | Solo si hay coincidencia exacta o palabra clave conocida | Puede inferir sobre descripciones nunca vistas, parecidas a las de entrenamiento |
| Trazabilidad de origen | Mapeo global por descripción, sin distinguir banco/manual por transacción | `categoria_modelo` / `categoria_usuario` / `metodo_vinculacion`, con trazabilidad fina |
| Infraestructura requerida | Baja — todo vive en Spring Boot | Mayor — serialización, Object Storage, modelo cacheado en la API |
| Cumplimiento del brief | No cubre por sí sola el requisito obligatorio de "clasificación supervisada" y "serialización de modelos" | Cubre ese requisito directamente |

---

## 4. Dónde gana cada propuesta (evaluación honesta)

**A favor de Backend:**
- Explicabilidad total — cada clasificación es auditable ("coincidió con la palabra JUMBO"), sin caja negra.
- Cero fricción para arrancar — no depende de que el modelo esté entrenado.
- Menor superficie de infraestructura a construir.

**A favor de Datos:**
- Generaliza a texto nunca visto, algo que un sistema de reglas exactas no puede hacer por diseño.
- Entrega una probabilidad real, tal como exige el ejemplo de salida del brief.
- Cumple un entregable obligatorio explícito del hackathon.
- Trazabilidad más fina del origen de cada categoría.

**Conclusión:** ninguna propuesta reemplaza a la otra — cubren debilidades distintas. Backend resuelve velocidad y explicabilidad en los casos obvios; Datos resuelve generalización y el requisito formal del brief en los casos que las reglas no alcanzan a cubrir.

---

## 5. Resultado de la unificación

Se optó por una **arquitectura híbrida de 4 niveles** (ver `plan-clasificacion-unificado.md`), donde el modelo de ML se inserta como un nivel intermedio, justo antes del fallback que ya existía en el código de Backend — es decir, un cambio incremental sobre lo ya construido, no una reescritura:

```
1. Mapeo exacto en BD          (Backend, ya diseñado)
2. Reglas de palabras clave     (Backend, ya diseñado)
3. Modelo ML entrenado          (Datos, en desarrollo)
4. OTROS_EGRESOS / OTROS_INGRESOS  (fallback final)
```

Ambas piezas de base de datos se mantienen, porque responden preguntas distintas: `transaction_category_mappings` da un beneficio inmediato y global a todos los usuarios; `categoria_modelo`/`categoria_usuario`/`metodo_vinculacion` da la trazabilidad fina necesaria para auditar y para alimentar el reentrenamiento del modelo.

El mecanismo `learnFromFeedback` que Backend ya implementó cumple doble función sin necesitar cambios: alimenta el mapeo en tiempo real **y** queda disponible como fuente de datos etiquetados por humanos para el próximo ciclo de entrenamiento del modelo.
