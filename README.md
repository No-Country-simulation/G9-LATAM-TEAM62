# Finance AI — Asistente Inteligente de Salud Financiera

Proyecto desarrollado en el marco del **Hackathon ONE G9** organizado por Alura Latam y Oracle.

---

## Descripción

Finance AI es una solución inteligente que analiza el comportamiento financiero de un usuario a partir de sus transacciones e información financiera, transformando datos en bruto en conocimiento útil y accionable.

El sistema recibe información relacionada con gastos, ingresos y hábitos financieros, y devuelve una evaluación completa del perfil financiero del usuario junto con recomendaciones personalizadas.

---

## Funcionalidades principales

- Clasificación automática de transacciones en categorías financieras
- Análisis del perfil financiero del usuario
- Generación de recomendaciones simples y objetivas
- Identificación de patrones de consumo
- Exposición de resultados mediante API REST en formato JSON

---

## Arquitectura general

La solución está compuesta por tres capas:

**Ciencia de Datos** — Construcción del dataset, exploración, ingeniería de atributos, entrenamiento y serialización de modelos de clasificación.

**Back-End** — API REST desarrollada en Java con Spring Boot. Recibe la información financiera, ejecuta las clasificaciones y devuelve respuestas estructuradas en JSON.

**Infraestructura OCI** — Uso de servicios Oracle Cloud Infrastructure para almacenamiento de modelos y despliegue de la aplicación.

---

## Ejemplo de uso

**Endpoint**

```
POST /analisis-financiero
```

**Request**

```json
{
  "ingreso_mensual": 4500,
  "nivel_endeudamiento": 25,
  "frecuencia_ahorro": "Media",
  "transacciones": [
    { "descripcion": "Supermercado", "valor": 420 },
    { "descripcion": "Combustible", "valor": 300 },
    { "descripcion": "Streaming", "valor": 40 }
  ]
}
```

**Response**

```json
{
  "perfil_financiero": "En observación",
  "probabilidad": 0.82,
  "resumen_gastos": {
    "alimentacion": 420,
    "transporte": 300,
    "entretenimiento": 40
  },
  "recomendaciones": [
    "Monitorear los gastos recurrentes de entretenimiento",
    "Aumentar la reserva financiera mensual"
  ]
}
```

---

## Stack tecnológico

| Área | Tecnologías |
|---|---|
| Ciencia de Datos | Python, Pandas, Scikit-Learn, Jupyter |
| Back-End | Java 17, Spring Boot 3, Maven |
| Infraestructura | OCI Object Storage, OCI Container Instances, Docker |
| Documentación API | springdoc-openapi (Swagger UI) |

---

## Estructura del repositorio

```
finance-ai/
├── docs/               # Documentación del proyecto y decisiones de arquitectura
├── data-science/       # Dataset, notebooks de EDA, entrenamiento y modelos serializados
├── backend/            # API REST Spring Boot
├── frontend/           # Interfaz de usuario
└── infra/              # Configuración Docker y OCI
```

---

## Categorías de clasificación

Las transacciones son clasificadas automáticamente en las siguientes categorías:

- Alimentación
- Transporte
- Salud
- Vivienda
- Educación
- Ocio
- Servicios

El perfil financiero del usuario se clasifica en: **Saludable**, **En observación** o **En riesgo**.

---

## Estado del proyecto

Proyecto en desarrollo activo — Semana 1 de 6.

---

## Programa

**ONE (Oracle Next Education)** — Hackathon Proyectos G9  
Organizado por Alura Latam y Oracle
