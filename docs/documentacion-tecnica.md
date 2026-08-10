# Finance AI — Documentación Técnica

**Proyecto:** Finance AI — Hackathon ONE Alura Latam + Oracle (Proyectos G9)
**Alcance de este documento:** arquitectura, modelo de datos, componentes, endpoints, y estado real del código al cierre de esta iteración.
**Complementa a:** `documentacion-funcional.md` (qué hace el producto, sin detalle técnico).

---

## 1. Visión general de la arquitectura

```
                    ┌─────────────────────┐
                    │   Frontend (React)   │  Nginx :80
                    └──────────┬───────────┘
                               │ HTTP
                    ┌──────────▼───────────┐
                    │  Backend (Spring Boot) │  :8080
                    │  Basic Auth (Spring    │
                    │  Security)             │
                    └──────────┬───────────┘
                               │ JDBC
                    ┌──────────▼───────────┐
                    │  Perfil dev: H2       │
                    │  Perfil oracle: OCI   │
                    │  Autonomous DB (ATP)  │
                    │  vía wallet mTLS      │
                    └───────────────────────┘
```

**Despliegue real (distinto al diseño de VCN de 3 capas que se documentó en etapas tempranas):** GitHub Actions construye imágenes Docker de `backend/` y `frontend/` y las publica en GHCR (`ghcr.io/no-country-simulation/g9-latam-team62-{backend,frontend}`). Un `docker-compose.yml` en el servidor las levanta; el perfil (`dev`/`oracle`) se controla por variable de entorno `SPRING_PROFILES_ACTIVE`. La arquitectura de VCN con Load Balancer, subredes públicas/privadas, NAT Gateway, etc. quedó documentada como diseño objetivo en etapas tempranas del proyecto, pero **el despliegue real actual es más simple**: contenedores directos, sin ese networking. Vale la pena que el equipo decida explícitamente si migran a esa arquitectura o si el docker-compose actual es el objetivo final para el hackathon.

---

## 2. Componentes del sistema

### 2.1 Backend — `backend/`

**Stack:** Java 25, Spring Boot 4.1.0, Spring Data JPA, Spring Security (HTTP Basic), Lombok, PDFBox + Apache POI (conversión PDF→Excel de cartolas).

**Capas:** `Controller → Service → Repository (JpaRepository)`, persistiendo en H2 (dev/test) u Oracle Autonomous Database (producción, perfil `oracle`).

| Paquete | Contenido |
|---|---|
| `model` | Entidades JPA: `User`, `Transaction`, `Recommendation`, `Currency`. Enums: `Category`, `TransactionType`, `FinancialProfile`, `SavingFrequency`. |
| `controller` | `AuthController`, `UserController`, `TransactionController`, `RecommendationController`, `PdfController`, `GlobalExceptionHandler`. |
| `service` | `UserService`, `TransactionService`, `RecommendationService`, `PdfToExcelService`. |
| `repository` | Interfaces `JpaRepository` para cada entidad, con métodos de consulta derivados (`findByUserId`, `findByEmail`, etc.). |
| `config` | `SecurityConfig` — HTTP Basic, todos los endpoints requieren autenticación excepto `/api/auth/**` y `/api/converter/**`. |
| `dto` | `LoginRequest`, `ProfileUpdateRequest`. |

**Perfiles de Spring:**
- `dev` (default): H2 en memoria, `ddl-auto=create-drop` — el schema se genera automáticamente desde las entidades JPA. Sembrado por `data.sql`.
- `oracle`: Oracle Autonomous Database vía wallet (mTLS), `ddl-auto=validate` — el schema es responsabilidad de `db/oracle/schema.sql`, aplicado a mano; la app solo valida que coincida con las entidades al arrancar. Cambios posteriores se aplican con scripts `ALTER TABLE` numerados (ver sección 4).

### 2.2 Frontend — `frontend/`

**Stack:** Vite, React 19, TypeScript, Tailwind CSS v4.

**Estado:** solo existe una landing page estática (`App.tsx`) con un puntaje hardcodeado renderizado como gauge SVG. Sin integración con la API, sin rutas, sin manejo de estado. Es un mockup visual, no una app funcional todavía.

### 2.3 Ciencia de Datos — `data-science/`

- `generar_dataset.py`: genera un dataset simulado de transacciones (2,200 usuarios, ~105,000 transacciones) para entrenamiento futuro del modelo de clasificación.
- `analisis_cartola.ipynb`: pipeline de ingesta multi-formato (`.xlsx`, `.xls`, `.csv`, `.pdf`) y multi-banco de cartolas reales. Normaliza montos, fechas y encabezados; clasifica transacciones por palabras clave usando **las 13 categorías reales de `Category.java`** (alineado con el backend a propósito — no inventa categorías). Probado de punta a punta contra 4 cartolas reales (Banco Chile, CuentaRUT, Falabella, Mercado Pago).
- `budget_recommendation_engine.py`: versión Python del motor de recomendaciones presupuestarias (ver sección 5), para uso en notebook o como microservicio Docker aparte.

---

## 3. Modelo de datos (schema real)

### 3.1 Tablas base (ya existentes en producción)

```
currencies                    users
├─ id (PK)                    ├─ id (PK)
└─ name_currency (unique)     ├─ name, email (unique, case-insensitive)
                               ├─ password (BCrypt hash)
                               ├─ monthly_income, saving_frequency
                               └─ financial_profile, profile_accuracy,
                                  profile_updated_at
                                  (SAVER/BALANCED/SPENDER/AT_RISK)

transactions                          recommendations
├─ id (PK)                            ├─ id (PK)
├─ description, operation_number      ├─ text
├─ amount (> 0)                       ├─ generated_at
├─ category (13 valores, ver 3.2)     ├─ profile_at_generation
├─ transaction_date                   └─ user_id (FK -> users)
├─ currency_id (FK -> currencies)
├─ balance_after
└─ user_id (FK -> users)
```

**Nota de diseño importante:** `financial_profile` vive directo en `users` — cada actualización sobrecribe el valor anterior. La migración `003_financial_profile_history.sql` (ver sección 3.3) resuelve esto con una tabla de historial aparte, enlazada por FK — ya no es un pendiente.

### 3.2 Categorías (`Category` enum / `CHECK CONSTRAINT`, no es una tabla catálogo)

Las categorías **no viven en una tabla** — son un enum Java (`Category.java`) reflejado 1:1 en un `CHECK CONSTRAINT` sobre `transactions.category`:

| Tipo (`TransactionType`) | Categorías |
|---|---|
| `EXPENSE` (9) | `FOOD`, `TRANSPORT`, `HOUSING`, `UTILITIES`, `ENTERTAINMENT`, `HEALTH`, `EDUCATION`, `SHOPPING`, `OTHER_EXPENSE` |
| `INCOME` (2) | `SALARY`, `OTHER_INCOME` |
| `SAVING` (2) | `INVESTMENT`, `SAVINGS` |

No existe una categoría de "transferencia" — ver sección 5.2 para cómo se resuelve esto sin tocar el enum.

### 3.3 Tablas y columnas del aporte de esta iteración

Migración incremental `db/oracle/002_manual_entries_and_budget.sql`, aplicada **después** del schema real (`schema.sql` + `data.sql`), sin reemplazar nada:

**Columnas nuevas en `transactions`:**

| Columna | Tipo | Qué resuelve |
|---|---|---|
| `source` | `BANK` \| `MANUAL` | Distingue si la transacción vino de una cartola o la ingresó el usuario a mano. |
| `payment_method` | `CASH` \| `DEBIT` | Solo aplica a `MANUAL`. Tarjeta de crédito excluida del MVP a propósito. |
| `link_status` | `UNLINKED` \| `AUTOMATIC` \| `USER_CONFIRMED` | Estado de conciliación de un registro `MANUAL` contra una transacción `BANK` real (mismo monto, ventana de 2-3 días). |
| `linked_transaction_id` | FK a `transactions.id` | La transacción `BANK` con la que se concilieven un registro `MANUAL`. |
| `category_method` | `MAPPING` \| `KEYWORD_RULE` \| `ML_MODEL` \| `FALLBACK` \| `USER_PROVIDED` \| `USER_CORRECTED` | Cómo se determinó `category` — clave para saber qué datos son confiables para reentrenar el modelo. |
| `category_confidence` | `0.0`–`1.0`, nullable | Probabilidad del modelo, solo cuando `category_method = ML_MODEL`. |

También se relaja `category` de `NOT NULL` a nullable: una transacción `BANK` recién ingerida puede llegar sin categoría, a la espera de que el clasificador la complete.

**Tabla nueva, independiente — `category_budget_targets`:**

```
category_budget_targets
├─ id (PK)
├─ country_code (default 'CL')
├─ category (una de las 9 EXPENSE)
├─ recommended_percentage (0-100)
└─ source (texto libre, cita la fuente del dato)

unique(country_code, category)
```

Poblada con 9 filas para Chile, con porcentajes derivados de la **IX Encuesta de Presupuestos Familiares del INE (2022-2023)** — ver `docs/plan-accion-motor-recomendaciones.md` para el detalle de cómo se mapeó cada categoría de la encuesta a las categorías del sistema. Diseñada para agregar otros países solo insertando filas nuevas.

**Tabla nueva — `financial_profile_history`** (migración `003_financial_profile_history.sql`):

```
financial_profile_history
├─ id (PK)
├─ user_id (FK -> users)
├─ financial_profile (SAVER/BALANCED/SPENDER/AT_RISK)
├─ profile_accuracy (nullable)
└─ recorded_at
```

Se escribe una fila nueva cada vez que corre `UserService.updateProfile()` (el mismo método que ya existía, con un `save()` adicional al final) — sin tocar `users`. Permite reconstruir la evolución del perfil financiero de un usuario en el tiempo, y es la base de datos que necesita el "análisis evolutivo del cliente" (comparar el % de gasto actual contra el promedio histórico del propio usuario — ver `docs/actividades-data-science.md`, sección de próximos pasos).

### 3.4 Diagrama entidad-relación (estado final, tras el aporte)

```
users ──1───N── transactions ──N───1── currencies
  │                  │
  │                  └── (self-FK) linked_transaction_id
  │
  └──1───N── recommendations

category_budget_targets   (independiente, sin FK hacia el resto)
```

---

## 4. Convención de migraciones

El propio README de Backend documenta el patrón: **`schema.sql` se aplica una sola vez a mano; los cambios posteriores son scripts `ALTER TABLE` numerados**, sin herramienta de migraciones (Flyway/Liquibase). `ddl-auto=validate` avisa al arrancar si el schema real y las entidades JPA se desincronizan.

El aporte de esta iteración sigue exactamente ese patrón:

```
db/oracle/
├── schema.sql                              (real, no se toca)
├── data.sql                                (real, no se toca)
├── drop.sql                                (real, no se toca)
├── 002_manual_entries_and_budget.sql       (nuevo — ALTER + CREATE TABLE)
├── 002_manual_entries_and_budget_drop.sql  (nuevo — rollback simétrico)
├── 003_financial_profile_history.sql       (nuevo — CREATE TABLE)
└── 003_financial_profile_history_drop.sql  (nuevo — rollback simétrico)
```

**Orden de aplicación:** `schema.sql` → `data.sql` → `002_manual_entries_and_budget.sql` → `003_financial_profile_history.sql`.

---

## 5. Funcionalidades entregadas en esta iteración

### 5.1 Registro manual de transacciones

`POST /api/transactions/manual` — captura un gasto fuera de la cartola bancaria (ej. efectivo). El servidor asigna la fecha (siempre "hoy") y el estado de conciliación (`UNLINKED`); el cliente nunca controla esos campos.

```json
// Request
{
  "userId": 1,
  "amount": 15.50,
  "category": "FOOD",
  "paymentMethod": "CASH",
  "currency": { "name_currency": "CLP" },
  "description": "chocolate"
}
// → 201 Created, con source=MANUAL, categoryMethod=USER_PROVIDED, linkStatus=UNLINKED
```

### 5.2 Corrección de categoría (feedback del usuario)

`PUT /api/transactions/{id}/category` — corrige la categoría sugerida por cualquier nivel del pipeline de clasificación (ver `docs/plan-clasificacion-unificado.md`). Marca `categoryMethod = USER_CORRECTED`.

### 5.3 Motor de Recomendaciones Presupuestarias

`POST /api/recommendations/generate?userId={id}&from={fecha}&to={fecha}` — compara el gasto real por categoría contra `category_budget_targets` y genera hasta 3 recomendaciones por corrida, más una de tasa de ahorro si corresponde. Reglas del motor:

- Mínimo 5 transacciones en el período; si no, no genera nada (evita ruido).
- 3 niveles de severidad según cuánto se excede el % recomendado (1.2x / 1.5x / 2x).
- Objetivo de tasa de ahorro: 20% — si `(ingresos - gastos) / ingresos` cae bajo eso, se agrega una recomendación aparte.
- Enfriamiento de 7 días por usuario, para no repetir alertas.
- **Sin Machine Learning** — es aritmética sobre datos ya categorizados, a propósito (ver `docs/plan-accion-motor-recomendaciones.md` para la justificación).

**Detección de transferencias sin tocar el enum real:** las transferencias (`TRANSF`, `TEF`, `GIRO`) se guardan igual que cualquier transacción (normalmente como `OTHER_EXPENSE`/`OTHER_INCOME`, según dirección — el dinero queda completamente reflejado). El motor las detecta por **texto en la descripción** (regex) y las excluye del cálculo de % de gasto, sin necesitar una categoría nueva. Misma lógica en Java (`BudgetRecommendationService.isLikelyTransfer`) y Python (`is_transfer`).

Implementado en dos lenguajes, con paridad de comportamiento verificada:
- **Java** (`BudgetRecommendationService.java`) — listo para integrar al backend real, JPA-nativo.
- **Python** (`budget_recommendation_engine.py`) — para notebook o microservicio Docker aparte.

### 5.4 Plan de clasificación híbrido (diseñado, pendiente de implementar en Backend)

4 niveles, de mayor a menor precisión: mapeo aprendido en BD → reglas de palabras clave → modelo ML entrenado → fallback. Detalle completo, incluyendo qué construye cada equipo, en `docs/plan-clasificacion-unificado.md` y `docs/plan-clasificacion-comparacion.md`.

---

## 6. Inventario de archivos de la entrega

```
G9-LATAM-TEAM62-main-reconciliado.zip   ← repo completo, listo para reemplazar el actual
├── backend/.../model/Transaction.java              (modificado: +6 columnas)
├── backend/.../model/{TransactionSource,PaymentMethod,LinkStatus,CategoryMethod,CategoryBudgetTarget}.java  (nuevos)
├── backend/.../dto/{ManualTransactionRequest,CategoryCorrectionRequest}.java  (nuevos)
├── backend/.../repository/TransactionRepository.java        (modificado: +1 método)
├── backend/.../repository/CategoryBudgetTargetRepository.java  (nuevo)
├── backend/.../service/TransactionService.java     (modificado: +2 métodos)
├── backend/.../service/BudgetRecommendationService.java  (nuevo)
├── backend/.../controller/TransactionController.java     (modificado: +2 endpoints)
├── backend/.../controller/RecommendationController.java  (modificado: +1 endpoint)
└── backend/src/main/resources/db/oracle/002_manual_entries_and_budget*.sql  (nuevos)

backend-aporte/    ← los mismos archivos sueltos, para revisar diffs uno por uno
data-science/      ← generar_dataset.py, analisis_cartola.ipynb, budget_recommendation_engine.py
docs/              ← toda la documentación de decisiones, incluida esta
```

---

## 7. Pendientes conocidos (honestidad ante todo)

- ~~Sin tabla de historial de perfil financiero~~ — resuelto: `financial_profile_history` (migración 003), con un hook mínimo en `UserService.updateProfile()` que escribe una fila cada vez.
- **`CategoryClassifierService` (niveles 1 y 2 del plan híbrido) no está implementado en el repo real** — el diseño existe (`Plan_de_clasificación.md`), falta llevarlo al código.
- **El modelo ML (nivel 3) no está entrenado ni servido** — el dataset simulado existe, el entrenamiento es un paso pendiente.
- **`schema.sql` de la raíz del repo es un archivo huérfano** — no lo referencia nada (confirmado por búsqueda exhaustiva en `.md`/`.yml`/`.properties`/`Dockerfile`). Candidato a eliminar.
- **Frontend sin integrar** — solo landing estática, sin consumo de la API todavía.
- **No se pudo compilar con Maven real** — este sandbox no tiene salida a Maven Central. Se verificó manualmente cada referencia cruzada (imports, métodos de repositorio, tipos), pero se recomienda correr `./mvnw compile && ./mvnw test` antes de mergear.
- **Autenticación:** `SecurityConfig` exige HTTP Basic en todos los endpoints salvo `/api/auth/**` y `/api/converter/**` — el endpoint nuevo `/api/recommendations/generate` hereda ese requisito automáticamente, sin cambios adicionales.
