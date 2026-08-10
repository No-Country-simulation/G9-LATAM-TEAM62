# Plan de Clasificación — Propuesta del Equipo de Datos

**Proyecto:** Finance AI — Hackathon ONE Alura Latam + Oracle
**Autor:** Equipo de Ciencia de Datos / Arquitectura
**Estado:** Propuesta para contrastar con la del equipo de Backend

---

## 1. Enfoque

Clasificación automática de transacciones mediante un **modelo de Machine Learning supervisado** (Scikit-Learn), entrenado sobre un dataset propio (simulado + validado contra cartolas reales), que predice la categoría de gasto a partir del texto de la descripción y entrega una probabilidad de confianza.

Este enfoque responde directamente a un requisito explícito del brief del hackathon: *"Entrenamiento y evaluación de modelos"*, *"clasificación supervisada"* y *"serialización de los modelos"* son entregables obligatorios de la sección de Ciencia de Datos.

---

## 2. Flujo de clasificación

1. Se normaliza el texto de la descripción (mayúsculas, sin tildes, sin números/códigos variables de sucursal u operación).
2. El texto normalizado se vectoriza (TF-IDF u otra técnica equivalente) y se pasa al modelo entrenado.
3. El modelo predice la categoría y una probabilidad asociada.
4. El resultado se guarda como `categoria_modelo` en la transacción.

---

## 3. Dos fuentes de dato, con trazabilidad de origen

- **`categoria_modelo`**: la predicción automática del modelo, aplicada a transacciones que provienen de una cartola bancaria cargada por el usuario.
- **`categoria_usuario`**: categoría de un gasto autoreportado manualmente por el usuario, fuera de lo que aparece en el banco.
- **`metodo_vinculacion`**: registra si un registro manual fue conciliado automáticamente contra una transacción bancaria real, confirmado por el usuario, o quedó sin vincular — permite distinguir datos de entrenamiento confiables de los que no.

Este nivel de trazabilidad es clave para poder auditar, más adelante, qué corrección vino de qué usuario y en qué transacción específica — no solo "esta descripción se corrigió alguna vez".

---

## 4. Ciclo de mejora del modelo

El modelo no es estático: se reentrena en ciclos (previsto para S3, y luego de forma periódica) incorporando:
- Las correcciones manuales de los usuarios (`categoria_usuario` confirmada)
- Nuevas cartolas reales analizadas, que amplían el vocabulario real de descripciones bancarias

---

## 5. Fortalezas de este enfoque

- **Generaliza a texto nunca visto**: puede acertar una descripción parecida-pero-distinta a algo visto en entrenamiento, algo que un sistema de reglas exactas no puede hacer.
- **Entrega una probabilidad real** por transacción, tal como pide el ejemplo de salida del brief (`"probabilidad": 0.82`).
- **Cumple el requisito obligatorio** de Ciencia de Datos del hackathon.
- **Trazabilidad fina** del origen de cada categoría (modelo vs. usuario, y cómo se vinculó).

## 6. Debilidades reconocidas

- Requiere que el dataset y el entrenamiento estén listos antes de poder clasificar nada — no funciona "desde el día 1".
- Es una caja más opaca que un sistema de reglas: una predicción del modelo es menos auditable a simple vista que "coincidió con la palabra JUMBO".
- Suma infraestructura: serialización del modelo, almacenamiento en OCI Object Storage, cacheo en la API.
