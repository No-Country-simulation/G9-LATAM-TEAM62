# **Proyecto:** Finance AI – Asistente Inteligente de Salud Financiera

**Revisión:** 4 | **Fecha:** 10 - 08 - 2026

## Ficha del documento

| Fecha | Rev. | Autor | Modificación |
| --- | --- | --- | --- |
| *23/07/26* | 1 | G9-LATAM-Team 62 | Creación ERS: backend, arquitectura OCI y reqs Hackathon ONE. |
| *29/07/26* | 2 | G9-LATAM-Team 62 | Actualización de estado de infraestructura: Confirmación de instancia OCI Compute y Oracle Autonomous DB ya aprovisionadas y desplegadas. |
| *02/08/26* | 3 | G9-LATAM-Team 62 | Especificación técnica de JWT y Motor de Clasificación de 4 Niveles. |
| *10/08/26* | 4 | G9-LATAM-Team 62 | **Integración de Ingesta Automatizada de Cartolas (Python CLI Multi-formato), Motor de Recomendaciones Presupuestarias (Benchmarking INE Chile), Registro Manual & Medios de Pago, Historial de Perfil Financiero y Extensión del Modelo de Datos.** |

---

## 1. Introducción

### 1.1. Propósito y Ámbito (MVP)

Solución inteligente que analiza la salud financiera del usuario a partir de sus transacciones y cartolas bancarias.

* **Funciones principales del sistema:**
  * **Ingesta Automatizada de Cartolas Multi-formato:** Procesamiento y extracción de transacciones desde archivos PDF, Excel (`.xlsx`, `.xls`) y CSV de múltiples entidades bancarias mediante integración híbrida Java-Python (`procesar_cartola_cli.py`).
  * **Clasificación Jerárquica de 4 Niveles:** Auto-clasificación de gastos integrando coincidencia exacta en BD (Mapeos colaborativos), reglas heurísticas de palabras clave (Keywords), inferencia con modelos de Machine Learning (Scikit-Learn/XGBoost) y fallback por defecto con registro trazable de scores de confianza (`category_confidence`) y método resolutor (`CategoryMethod`).
  * **Aprendizaje Continuo y Colaborativo (Crowdsourcing / Feedback Loop):** Retroalimentación en tiempo real cuando el usuario corrige manualmente una categoría (`PUT /api/transactions/{id}/category`), actualizando las reglas globales para beneficio de todos los usuarios y generando datasets auditados para reentrenamiento de Data Science.
  * **Motor de Recomendaciones Presupuestarias & Analítica (Benchmarking INE Chile):** Comparación estadística del consumo del usuario contra la IX Encuesta de Presupuestos Familiares del INE (Chile), filtrado inteligente de transferencias internas, cálculo de la tasa de ahorro y control de cooldown (máximo 3 sugerencias por corrida, 7 días de enfriamiento).
  * **Registro Manual & Medios de Pago:** Capacidad de ingresar movimientos no bancarizados especificando el medio de pago (*CASH*, *DEBIT*, *CREDIT*) y origen (*BANK*, *MANUAL*).
  * **Trazabilidad de Historial de Perfil Financiero:** Auditoría histórica de cambios de perfil (*SAVER*, *BALANCED*, *SPENDER*, *AT_RISK*) y nivel de precisión en el tiempo.
  * **Autenticación Stateless JWT & API REST:** Seguridad mediante Spring Security, tokens JWT Bearer, hashing BCrypt y documentación interactiva mediante Swagger/OpenAPI.

### 1.2. Glosario

* **JWT / BCrypt:** Estándar de tokens stateless (JSON Web Token) y hashing criptográfico seguro de contraseñas.
* **Crowdsourcing / Feedback Loop:** Aprendizaje continuo colaborativo alimentado por las correcciones directas de los usuarios.
* **INE Chile:** Instituto Nacional de Estadísticas de Chile; fuente oficial de la Encuesta de Presupuestos Familiares para benchmarking financiero.
* **ProcessBuilder:** API de Java para la ejecución segura de subprocesos aislados en el sistema operativo (invocación de CLI Python).
* **CategoryMethod:** Enumeración que identifica el mecanismo resolutor de la categoría (*EXACT_MAPPING*, *KEYWORD_RULE*, *ML_MODEL*, *FALLBACK*, *USER_PROVIDED*, *USER_CORRECTED*).
* **OCI / VCN:** Oracle Cloud Infrastructure y Red Virtual Nube (Virtual Cloud Network).
* **Cartola:** Documento bancario (PDF, Excel o CSV) que registra el estado de cuenta y movimientos de un cliente.

---

## 2. Descripción General y Arquitectura

### 2.1. Arquitectura de 3 Capas en OCI con Integración Híbrida Python

Toda la infraestructura base en Oracle Cloud Infrastructure se encuentra **aprovisionada, operativa y desplegada**:

```
[ Cliente API / Frontend ]
          │
          ├── HTTP / REST (JWT Bearer Token)
          ▼
┌────────────────────────────────────────────────────────────────────────┐
│ Capa 1 (Subred Pública OCI)                                           │
│ Internet Gateway ──► Load Balancer Público ──► Static Dashboard Assets │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ Capa 2 (Subred Privada - Aplicación & Lógica de Negocio)               │
│ Instancia OCI Compute:                                                 │
│   ├── API REST Spring Boot 4.1 + Spring Security + JWT                 │
│   ├── StatementIngestionService ──(ProcessBuilder)──► Python CLI       │
│   │                                            (procesar_cartola_cli)  │
│   ├── CategoryClassifierService (Motor 4 Niveles + Feedback Loop)       │
│   └── BudgetRecommendationService (Benchmarking INE Chile + Cooldown)  │
│ NAT Gateway para salidas requeridas y parches.                         │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ Capa 3 (Subred Privada - Datos & Persistencia)                         │
│   ├── Oracle Autonomous Database (Users, Transactions, Mappings,       │
│   │   Keywords, Budget Targets INE, Profile History)                  │
│   └── OCI Object Storage (Artefactos y Modelos de ML serializados)     │
└────────────────────────────────────────────────────────────────────────┘
```

* **Capa 1 (Subred Pública):** Internet Gateway, Load Balancer Público y assets del dashboard.
* **Capa 2 (Subred Privada - Aplicación):** Instancia *OCI Compute* alojando la API REST en Spring Boot 4.1 + Spring Security + JWT. Incluye el módulo de orquestación de subprocesos Python (`procesar_cartola_cli.py` mediante `ProcessBuilder`) y NAT Gateway para conexiones externas seguras.
* **Capa 3 (Subred Privada - Datos):** *Oracle Autonomous Database* provista para la persistencia de usuarios, transacciones, reglas colaborativas, metas presupuestarias INE e historial de perfiles, junto a *OCI Object Storage* para artefactos y modelos de Machine Learning.

### 2.2. Módulos API REST

1. **Autenticación y Seguridad (`/api/auth`):**
   * `POST /api/auth/register`: Registro de usuarios.
   * `POST /api/auth/login`: Autenticación y generación de token JWT Bearer.
   * `PUT /api/auth/change-password`: Cambio seguro de contraseña.
2. **Ingesta y Transacciones (`/api/transactions`):**
   * `POST /api/transactions/upload-statement`: Carga multipart de cartolas bancarias (PDF, XLSX, XLS, CSV) y procesamiento automatizado con Python CLI.
   * `POST /api/transactions/manual`: Registro de transacciones manuales (efectivo, débito, crédito).
   * `GET /api/transactions` & `GET /api/transactions/{id}`: Consulta de movimientos del usuario.
   * `PUT /api/transactions/{id}/category`: Corrección de categoría por parte del usuario y disparo del aprendizaje por retroalimentación en tiempo real (`learnFromFeedback`).
3. **Analítica y Recomendaciones Presupuestarias (`/api/recommendations`):**
   * `POST /api/recommendations/generate`: Evaluación de gastos por período contra el benchmark del INE Chile, filtrado de transferencias internas y generación de sugerencias con control de cooldown.
   * `GET /api/recommendations/user/{userId}`: Historial de recomendaciones del usuario.
4. **Perfilamiento y Trazabilidad (`/api/users`):**
   * `GET /api/users/{userId}`: Perfil actual del usuario (*SAVER*, *BALANCED*, *SPENDER*, *AT_RISK*).
   * `GET /api/users/{userId}/profile-history`: Historial de cambios de perfil y precisión en el tiempo.

---

## 3. Especificación Técnica

### 3.1. Autenticación Stateless JWT

* **Filtro (`JwtAuthenticationFilter`):** Intercepta cada petición HTTP, extrae el token del encabezado `Authorization: Bearer <token>`, valida la firma criptográfica con `JwtService` y establece el objeto `Authentication` en el `SecurityContextHolder`.
* **Seguridad de Contraseñas:** Contraseñas cifradas con **BCrypt** (`BCryptPasswordEncoder`) y declaradas como `WRITE_ONLY` en DTOs.
* **Control de Accesos:** Rutas públicas (`/api/auth/**`, Swagger UI `/swagger-ui/**`) vs. Rutas protegidas (requieren token Bearer activo).

---

### 3.2. Ingesta Automatizada de Cartolas (Python CLI Integration)

Para procesar heterogéneos formatos de cartolas bancarias sin recargar el núcleo Java, el servicio `StatementIngestionService` interactúa con un ejecutable Python aislado (`procesar_cartola_cli.py`):

1. **Flujo de Ejecución:**
   * El cliente realiza un `POST /api/transactions/upload-statement` enviando un archivo multipart.
   * `StatementIngestionService` genera un archivo temporal en disco (`cartola_XXXX.tmp`).
   * Se ejecuta mediante `ProcessBuilder` el comando:
     ```bash
     python backend/scripts/procesar_cartola_cli.py <ruta_temp> --anio-defecto 2026 --pais CL
     ```
2. **Capacidades del Script Python:**
   * **Detección de Banco Origen:** Reconocimiento automático de patrones de encabezado y tablas para *Banco de Chile*, *Santander*, *CuentaRUT / BancoEstado*, *Banco Falabella*, *Mercado Pago*, *BCI*, *Itaú* y *Scotiabank*.
   * **Parsing Vectorial y Tabular:** Utiliza `pdfplumber` para la extracción de tablas en PDF basadas en coordenadas físicas y bordes vectoriales; utiliza `pandas` y `openpyxl` para hojas de cálculo Excel (`.xlsx`, `.xls`) y CSV.
   * **Normalización de Datos:** Limpieza de montos (remoción de puntos/comas, formateo de paréntesis contables `(100)` $\rightarrow$ `-100`), normalización de fechas y detección de días festivos mediante la librería `holidays`.
   * **Salida Estandarizada:** Emisión en `stdout` de un objeto JSON estructurado conteniendo conteo de filas válidas/descartadas, advertencias y la lista de transacciones parseadas.
3. **Limpieza Garantizada:** El archivo temporal en disco se elimina indefectiblemente en el bloque `finally` del servicio Spring Boot.

---

### 3.3. Motor Jerárquico de Clasificación de 4 Niveles y Aprendizaje Colaborativo

Procesamiento ordenado de descripciones mediante `CategoryClassifierService` y `TextNormalizer`:

1. **Normalización (`TextNormalizer`):** Conversión a mayúsculas, eliminación de tildes/diacríticos, remoción de RUTs, fechas, números de operación y secuencias variables, colapsando espacios múltiples.
2. **Cadena de 4 Niveles con Trazabilidad:**

| Nivel | Componente / Mecanismo | Origen | Descripción / Criterio | Confianza (`categoryConfidence`) | Método (`CategoryMethod`) |
| :---: | :--- | :---: | :--- | :---: | :---: |
| **N1** | **Mapeo Colaborativo (BD)** | Base de Datos | Coincidencia exacta con patrones en `transaction_category_mappings` aprendidos por retroalimentación de usuarios. | `1.0` | `EXACT_MAPPING` |
| **N2** | **Reglas de Keywords (BD)** | Base de Datos | Búsqueda parcial de palabras clave en `category_keywords` (>150 keywords registradas). | `0.9` | `KEYWORD_RULE` |
| **N3** | **Modelo ML Supervisado** | Microservicio ML | Inferencia vía `MlInferenceService` (Scikit-Learn/XGBoost) si la confianza devuelta es $\ge 0.60$. | Variable ($\ge 0.60$) | `ML_MODEL` |
| **N4** | **Fallback Jerárquico** | Backend | Asignación por defecto a `OTHER_EXPENSE` u `OTHER_INCOME` si ningún nivel anterior superó los umbrales. | `0.0` | `FALLBACK` |

3. **Ciclo de Aprendizaje Continuo (`learnFromFeedback`):**
   * Al ejecutar `PUT /api/transactions/{id}/category`, se actualiza la categoría de la transacción y se invoca `learnFromFeedback()`.
   * El sistema inserta o actualiza el patrón normalizado en `transaction_category_mappings` e incrementa su contador de frecuencia (`frequency`).
   * **Impacto Inmediato:** Futuras transacciones idénticas ingresadas por **cualquier usuario** se clasificarán instantáneamente en **Nivel 1**.
   * **Auditoría para Reentrenamiento:** La transacción mantiene registro de la categoría original predicha vs. la corregida por el usuario, consolidando datasets auditables para el reentrenamiento offline de los modelos de Data Science.

---

### 3.4. Motor de Recomendaciones Presupuestarias y Analítica (INE Chile)

El servicio `BudgetRecommendationService` aplica análisis estadístico comparativo para promover la salud financiera:

1. **Benchmarking Gubernamental (INE Chile):** Compara los porcentajes de gasto mensual del usuario por categoría contra la **IX Encuesta de Presupuestos Familiares del INE (Chile)** almacenados en `category_budget_targets`:
   * *Alimentos y Bebidas (`FOOD`):* Meta 21.3%
   * *Vivienda (`HOUSING`):* Meta 14.5%
   * *Transporte (`TRANSPORT`):* Meta 14.1%
   * *Servicios Básicos (`UTILITIES`):* Meta 6.2%
   * *Salud (`HEALTH`):* Meta 7.4%
   * *Educación (`EDUCATION`):* Meta 6.5%
   * *Entretenimiento (`ENTERTAINMENT`):* Meta 5.1%
   * *Compras y Vestuario (`SHOPPING`):* Meta 4.8%
2. **Filtrado Inteligente de Ruido:** Excluye automáticamente transferencias internas y giros mediante expresiones regulares (`TRANSF`, `TEF`, `GIRO`, `PAGO DE TARJETA`) para no distorsionar la medición del consumo real.
3. **Evaluación de Tasa de Ahorro:** Mide la capacidad de ahorro del período comparándola contra el objetivo recomendado del 20% (`TARGET_SAVINGS_RATE = 0.20`).
4. **Control de Cooldown y Límites:**
   * Generación máxima de 3 sugerencias prioritarias por corrida (`MAX_RECOMMENDATIONS_PER_RUN = 3`).
   * Período de enfriamiento (*cooldown*) de 7 días entre evaluaciones para evitar la saturación de alertas al usuario.

---

## 4. Requisitos Específicos y Modelo de Datos

### 4.1. Esquema Lógico de Datos (En Oracle Autonomous DB)

* **`users`:** `id`, `name`, `email`, `password` (BCrypt), `monthly_income`, `saving_frequency`, `financial_profile`, `profile_accuracy`, `profile_updated_at`.
* **`transactions`:** `id`, `description`, `operation_number`, `amount`, `category`, `transaction_date`, `currency_id`, `balance_after`, `user_id`, `source` (*BANK*, *MANUAL*), `payment_method` (*CASH*, *DEBIT*, *CREDIT*), `bank_name`, `category_method` (*EXACT_MAPPING*, *KEYWORD_RULE*, *ML_MODEL*, *FALLBACK*, *USER_PROVIDED*, *USER_CORRECTED*), `category_confidence`.
* **`recommendations`:** `id`, `text`, `generated_at`, `profile_at_generation`, `user_id`.
* **`category_budget_targets`:** `id`, `category`, `target_percentage`, `description`.
* **`financial_profile_history`:** `id`, `user_id`, `financial_profile`, `profile_accuracy`, `created_at`.
* **`transaction_category_mappings`:** `id`, `description_pattern`, `category`, `frequency`.
* **`category_keywords`:** `id`, `keyword`, `category`.
* **Enums y Catálogos:**
  * `financial_profile`: *SAVER*, *BALANCED*, *SPENDER*, *AT_RISK*.
  * `transaction_category`: *FOOD*, *TRANSPORT*, *HOUSING*, *UTILITIES*, *ENTERTAINMENT*, *HEALTH*, *EDUCATION*, *SHOPPING*, *SALARY*, *SAVINGS*, *INVESTMENT*, *OTHER_INCOME*, *OTHER_EXPENSE*.
  * `transaction_type`: *INCOME*, *EXPENSE*, *SAVING*.
  * `TransactionSource`: *BANK*, *MANUAL*.
  * `PaymentMethod`: *CASH*, *DEBIT*, *CREDIT*.
  * `CategoryMethod`: *EXACT_MAPPING*, *KEYWORD_RULE*, *ML_MODEL*, *FALLBACK*, *USER_PROVIDED*, *USER_CORRECTED*.

---

### 4.2. Requisitos Funcionales y No Funcionales

* **RF1 - Autenticación JWT:** Registro, inicio de sesión seguro devolviendo token JWT Bearer y protección de rutas.
* **RF2 - Ingesta Cartolas Multi-formato:** Carga y extracción automatizada de movimientos bancarios en PDF, Excel y CSV mediante subproceso Python CLI (`procesar_cartola_cli.py`).
* **RF3 - Clasificación Jerárquica & Trazabilidad:** Auto-clasificación en 4 niveles registrando score de confianza y método resolutor.
* **RF4 - Retroalimentación y Aprendizaje Colaborativo:** Corrección de categorías por usuario (`PUT /api/transactions/{id}/category`) alimentando las reglas globales N1 y datasets de Data Science.
* **RF5 - Motor de Recomendaciones Presupuestarias (INE Chile):** Generación de alertas estadísticas basadas en la Encuesta de Presupuestos Familiares del INE con control de cooldown (7 días, máx 3 alertas).
* **RF6 - Transacciones Manuales & Medios de Pago:** Registro manual de egresos/ingresos no bancarizados especificando medio de pago (*CASH*, *DEBIT*, *CREDIT*).
* **RF7 - Historial de Perfil Financiero:** Auditoría histórica de la evolución del perfil de salud financiera del usuario.
* **RNF1 - Seguridad Stateless y Cifrado:** Spring Security + JWT y passwords hasheadas con BCrypt (`WRITE_ONLY`).
* **RNF2 - Escalabilidad Dinámica:** Gestión de reglas de palabras clave y metas INE en BD sin necesidad de redespliegue de código.
* **RNF3 - Infraestructura Cloud OCI:** Despliegue activo en arquitectura de 3 capas aisladas en OCI (Subredes Pública/Privada, OCI Compute y Oracle Autonomous DB).
* **RNF4 - Integración Híbrida Segura:** Invocación de subprocesos aislados en Java con borrado de temporales garantizado.
* **RNF5 - Documentación API:** Documentación completa e interactiva mediante Swagger UI (`/swagger-ui/index.html`).