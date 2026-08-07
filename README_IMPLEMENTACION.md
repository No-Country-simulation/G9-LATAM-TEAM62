# Finance AI — Backend & Motor de Clasificación e Ingesta

Este documento describe la arquitectura, endpoints y flujo funcional de la implementación del Backend en **Spring Boot**, que unifica el **Motor Jerárquico de Clasificación de 4 Niveles** con los módulos desarrollados por Data Science: **Ingesta Automatizada de Cartolas Bancarias (Python CLI)**, **Motor de Recomendaciones Presupuestarias (INE)**, **Registro Manual & Corrección Colaborativa**, e **Historial de Perfil Financiero**.

---

## 1. Resumen Ejecutivo de la Arquitectura

La solución combina un núcleo robusto en Java con un script auxiliar en Python para procesar documentos complejos:

```
[ Frontend / Cliente API ]
        │
        ├── Multi-part Upload (PDF, XLSX, CSV)
        ▼
[ StatementIngestionService ] ──(ProcessBuilder)──► [ procesar_cartola_cli.py ]
        │                                                     │
        │ ◄────────────────── JSON de salida ─────────────────┘
        ▼
[ CategoryClassifierService ] (4 Niveles)
  ├── 1. Mapeo Colaborativo (BD)
  ├── 2. Reglas de Keywords (BD)
  ├── 3. Modelo de ML (Stub / Service)
  └── 4. Fallback (OTROS_EGRESOS / OTROS_INGRESOS)
        │
        ▼
[ Base de Datos ] (Transactions, Budget Targets, History, Mappings)
        │
        ▼
[ BudgetRecommendationService ] ──► Alertas y Recomendaciones (INE)
```

---

## 2. Funcionalidades Principales

### 2.1. Ingesta Automatizada de Cartolas Bancarias (`Python CLI`)
- **Formatos Soportados**: `.pdf`, `.xlsx`, `.xls`, `.csv`.
- **Detección Automática**: Identifica el banco origen (`BANCO_CHILE`, `CUENTA_RUT`, `FALABELLA`, `MERCADO_PAGO`, `SANTANDER`, `BCI`, `ITAU`, `SCOTIABANK`), el país (`CL`) y el año.
- **Normalización**: Limpia montos (gestión de signos, paréntesis contables, comas/puntos), fechas y detecta días feriados.
- **Seguridad**: Ejecución aislada por subproceso en Java (`ProcessBuilder`) con borrado garantizado de archivos temporales.

### 2.2. Motor de Clasificación Jerárquica de 4 Niveles
Cada transacción procesada se evalúa secuencialmente a través de 4 niveles:
1. **Nivel 1 (Mapeo Exacto Colaborativo)**: Busca coincidencia previa aprendida a partir de correcciones humanas de los usuarios.
2. **Nivel 2 (Reglas de Palabras Clave)**: Evalúa más de 150 keywords en base de datos (`category_keywords`).
3. **Nivel 3 (Modelo de Machine Learning)**: Invocación al servicio de inferencia supervisada (`MlInferenceService`).
4. **Nivel 4 (Fallback)**: Asignación por defecto a `OTHER_EXPENSE` u `OTHER_INCOME`.

### 2.3. Motor de Recomendaciones Presupuestarias (INE Chile)
- **Benchmarking Gubernamental**: Compara la proporción de gasto mensual del usuario contra las metas de la IX Encuesta de Presupuestos Familiares del INE (Chile).
- **Filtrado Inteligente**: Detecta y excluye transferencias internas (`TEF`, `GIRO`, `TRANSF`) para no distorsionar el cálculo de consumo real.
- **Tasa de Ahorro**: Evalúa la capacidad de ahorro del período comparándola contra el objetivo del 20%.
- **Control de Cooldown**: Limita las recomendaciones a un máximo de 3 por corrida y aplica un enfriamiento de 7 días entre evaluaciones para evitar saturación al usuario.

### 2.4. Registro Manual & Corrección Colaborativa
- **Transacciones Manuales**: Permite registrar compras en efectivo o débito no bancarizadas.
- **Feedback Continuo (`learnFromFeedback`)**: Cada vez que un usuario corrige la categoría asignada a una transacción, el sistema actualiza automáticamente el Nivel 1 para que todos los usuarios se beneficien de esa corrección en el futuro.

### 2.5. Historial de Perfil Financiero
- Auditabilidad y trazabilidad histórica del cambio de perfil de riesgo/salud financiera del usuario en el tiempo.

---

## 3. Guía de Endpoints API

### 3.1. Transacciones

#### `POST /api/transactions/upload-statement`
Subida e ingesta de cartola bancaria en formato multipart.
- **Consumes**: `multipart/form-data`
- **Parámetros Form**:
  - `file`: Archivo en formato PDF, XLSX, XLS o CSV *(Obligatorio)*.
  - `userId`: ID del usuario *(Obligatorio)*.
  - `defaultYear`: Año por defecto *(Opcional)*.
  - `country`: Código ISO del país (`CL`) *(Opcional)*.
- **Respuesta `200 OK`**:
```json
{
  "status": "ok",
  "fileName": "Cartola_BancoChile.pdf",
  "country": "CL",
  "year": 2026,
  "rawRowsCount": 42,
  "validRowsCount": 38,
  "discardedRowsCount": 4,
  "warnings": [],
  "createdTransactions": [
    {
      "id": 101,
      "description": "SUPERMERCADO JUMBO",
      "amount": 45900.00,
      "category": "FOOD",
      "date": "2026-08-01",
      "balanceAfter": 1250000.00,
      "userId": 1,
      "source": "BANK",
      "bankName": "BANCO_CHILE",
      "categoryMethod": "KEYWORD_RULE",
      "categoryConfidence": 0.9
    }
  ]
}
```

#### `POST /api/transactions/manual`
Registra un gasto o egreso manual.
- **Body**:
```json
{
  "userId": 1,
  "amount": 15000.00,
  "category": "FOOD",
  "description": "Feria local verduras",
  "currency": { "id": 1, "name_currency": "CLP" },
  "paymentMethod": "CASH",
  "bankName": null
}
```

#### `PUT /api/transactions/{id}/category`
Corrige la categoría de una transacción y retroalimenta el motor colaborativo.
- **Body**:
```json
{
  "category": "HEALTH"
}
```

---

### 3.2. Recomendaciones

#### `POST /api/recommendations/generate`
Genera alertas y recomendaciones basadas en el gasto real del período.
- **Query Params**:
  - `userId`: ID del usuario *(Obligatorio)*.
  - `periodStart`: Fecha inicio (`YYYY-MM-DD`) *(Opcional, default: hace 30 días)*.
  - `periodEnd`: Fecha fin (`YYYY-MM-DD`) *(Opcional, default: hoy)*.
- **Respuesta `200 OK`**:
```json
[
  {
    "id": 15,
    "text": "Gasto bastante por sobre lo recomendado en transporte: 22.5% de tu gasto total (referencia: 14.1%). Considera revisar los gastos recurrentes en esta categoría.",
    "generatedAt": "2026-08-06T20:55:00",
    "profileAtGeneration": "BALANCED",
    "userId": 1
  }
]
```

---

## 4. Modelo de Datos & Esquema BD

### Tabla `transactions`
| Campo | Tipo | Descripción |
| :--- | :--- | :--- |
| `id` | `BIGINT` | Clave primaria autogenerada |
| `description` | `VARCHAR(255)` | Glosa original de la transacción |
| `amount` | `DECIMAL(19,2)` | Monto (siempre positivo) |
| `category` | `VARCHAR(50)` | Categoría asignada (`FOOD`, `TRANSPORT`, `HEALTH`, etc.) |
| `transaction_date` | `DATE` | Fecha del movimiento |
| `source` | `VARCHAR(10)` | Origen: `BANK` o `MANUAL` |
| `payment_method` | `VARCHAR(20)` | Medio de pago: `CASH`, `DEBIT`, `CREDIT` |
| `bank_name` | `VARCHAR(100)` | Entidad emisora (ej: `BANCO_CHILE`, `CUENTA_RUT`, `FALABELLA`, `MERCADO_PAGO`) |
| `category_method` | `VARCHAR(20)` | Nivel que resolvió la categoría (`EXACT_MAPPING`, `KEYWORD_RULE`, `ML_MODEL`, `FALLBACK`, `USER_PROVIDED`, `USER_CORRECTED`) |
| `category_confidence`| `DOUBLE` | Confianza de la predicción (0.0 a 1.0) |

### Tabla `category_budget_targets`
Metas de referencia INE Chile (`FOOD`: 21.3%, `HOUSING`: 14.5%, `TRANSPORT`: 14.1%, `UTILITIES`: 6.2%, `HEALTH`: 7.4%, `EDUCATION`: 6.5%, `ENTERTAINMENT`: 5.1%, `SHOPPING`: 4.8%).

### Tabla `financial_profile_history`
Registro histórico de cambios en el perfil del usuario (`user_id`, `financial_profile`, `profile_accuracy`, `created_at`).

---

## 5. Requisitos del Entorno (Python & Java)

### Java (Backend App)
- **Java 21 / 25**
- **Spring Boot 3.4+ / 4.x**
- **Maven Wrapper (`mvnw`)**

### Python (Para Script de Ingesta CLI)
- **Python 3.11+**
- Librerías requeridas:
  ```bash
  pip install pandas openpyxl xlrd pdfplumber holidays
  ```

---

## 6. Comandos de Compilación y Verificación

### Compilar el proyecto:
```bash
./mvnw clean compile
```

### Ejecutar Pruebas Unitarias e Integración:
```bash
./mvnw test
```

### Ejecutar la aplicación localmente:
```bash
./mvnw spring-boot:run
```
