**Proyecto:** Finance AI – Asistente Inteligente de Salud Financiera

**Revisión:** [ 2 ]
**Fecha:** 02 - 08 - 2026

## Ficha del documento
| **Fecha** | **Revisión** | **Autor** | **Modificación** |
| ------ | ------ | ------ | ------ |
| *23 / 07 / 26* | *1* | *Equipo Finance AI* | *Creación del documento ERS integrando el resumen del backend, arquitectura OCI y requerimientos del Hackathon ONE.* |
| *02 / 08 / 26* | *2* | *Equipo Finance AI* | *Incorporación de especificación técnica de Autenticación Stateless JWT y Motor de Clasificación Jerárquico de 4 Niveles con Aprendizaje Colaborativo.* |

---

## 1. Introducción
Este documento presenta el registro y la definición de los requisitos para el desarrollo del proyecto **Finance AI**, una solución orientada a mejorar la salud financiera de los usuarios. Se incluyen los requisitos, características del modelo de datos, detalles del backend, seguridad y la arquitectura de infraestructura en la nube.

### 1.1. Propósito
El propósito de este documento es detallar los requisitos de **"Finance AI"**, una solución inteligente capaz de **analizar el comportamiento financiero de un usuario a partir de sus transacciones e información financiera**. El sistema busca organizar gastos, identificar hábitos, clasificar automáticamente transacciones, determinar el perfil financiero y entregar recomendaciones simples para mejorar la gestión del dinero. 

### 1.2. Ámbito del Sistema
El proyecto corresponde a un **Asistente Inteligente de Salud Financiera** desarrollado como un Producto Mínimo Viable (MVP).
**Funciones principales del sistema:**
*   Obtención de datos financieros (ingresos, nivel de endeudamiento) y transacciones a través de las **cartolas de los clientes** (PDF y Excel).
*   **Clasificación automática e inteligente de gastos** mediante una cadena jerárquica de 4 niveles (coincidencias aprendidas en BD, reglas por palabras clave, inferencia de modelos ML y fallback por defecto).
*   **Aprendizaje continuo y colaborativo (Crowdsourcing):** El sistema aprende automáticamente cada vez que un usuario corrige una categoría de transacción.
*   **Autenticación Stateless mediante tokens JWT** (JSON Web Tokens) y cifrado BCrypt de contraseñas.
*   Evaluación y clasificación del perfil financiero del usuario (*SAVER*, *BALANCED*, *SPENDER*, *AT_RISK*).
*   Generación de recomendaciones financieras personalizadas y accionables.
*   Disposición de los resultados a través de una **API REST documentada en Swagger/OpenAPI**.

### 1.3. Definiciones, Acrónimos y Abreviaturas
*   **API REST:** Interfaz de Programación de Aplicaciones basada en el protocolo HTTP.
*   **JWT:** JSON Web Token, estándar para la autenticación segura y sin estado (stateless).
*   **BCrypt:** Algoritmo de hashing seguro utilizado para proteger las contraseñas de los usuarios.
*   **Crowdsourcing / Feedback:** Mecanismo de aprendizaje colaborativo donde las correcciones de los usuarios realimentan el sistema.
*   **OCI:** Oracle Cloud Infrastructure, la nube donde se desplegará el sistema.
*   **VCN:** Virtual Cloud Network, red virtual principal que aislará los recursos en OCI.
*   **MVP:** Producto Mínimo Viable.
*   **EDA:** Exploración y limpieza de datos (Exploratory Data Analysis).
*   **Cartola:** Documento bancario en PDF y Excel que registra los movimientos y transacciones de un cliente.

### 1.4. Referencias
*   Hackathon ONE – Proyectos G9 | Alura + Oracle.
*   Documentación de Spring Boot 4.1, Spring Security y JJWT (Java JWT).
*   Diseño de Arquitectura de 3 capas en OCI.

---

## 2. Descripción General

### 2.1. Perspectiva del Producto
El sistema se concibe bajo una **arquitectura de 3 capas en OCI** desplegada dentro de una VCN, separando la entrada de red, la lógica de negocio y el almacenamiento para garantizar seguridad y alto rendimiento:
*   **Capa 1: Entrada de Red (Subred Pública):** Expuesta a usuarios y aplicaciones cliente. Utiliza un *Internet Gateway* y un *Load Balancer Público* para distribuir el tráfico. Incluye recursos estáticos del dashboard.
*   **Capa 2: Aplicación y Lógica de Negocio (Subred Privada):** Aislada de internet. Contiene máquinas virtuales (*OCI Compute*) que alojan la API REST en Spring Boot 4.1 con Spring Security + JWT. Incluye un *NAT Gateway* para que los servidores descarguen dependencias bloqueando el tráfico entrante malicioso.
*   **Capa 3: Datos y Almacenamiento (Subred Privada):** Completamente aislada, solo recibe peticiones de la Capa 2. Utiliza *Oracle Autonomous Database* (para perfiles, usuarios, transacciones, mapeos de categorías y palabras clave) y *OCI Object Storage* (para guardar los modelos de ML serializados).

### 2.2. Funciones del Producto
El núcleo del sistema expone servicios para:
1.  **Autenticación y Seguridad (`/api/auth`):** Registro (`/register`), inicio de sesión (`/login`) con generación de token JWT bearer de corta/mediana duración, y cambio seguro de contraseña (`/change-password`).
2.  **Conversión de Cartolas (`/api/converter/pdf-to-excel`):** Carga de cartolas bancarias en PDF, extracción de texto (Apache PDFBox) y conversión a hoja de cálculo (.xlsx).
3.  **Gestión y Clasificación de Transacciones (`/api/transactions`):**
    *   `POST /api/transactions`: Registro de transacciones con auto-clasificación mediante el motor de 4 niveles.
    *   `PUT /api/transactions/{id}/category`: Corrección de categoría por parte del usuario y disparo del aprendizaje por retroalimentación en tiempo real.
4.  **Perfilamiento Financiero y Recomendaciones (`/api/users`, `/api/recommendations`):** Cálculo del perfil financiero del usuario y generación de recomendaciones personalizadas.

---

## 3. Especificación Técnica de Componentes Clave

### 3.1. Autenticación y Seguridad JWT
El sistema utiliza una arquitectura de seguridad sin estado (**Stateless Authentication**) basada en **Spring Security** y **JJWT** (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`):

*   **Filtro de Autenticación (`JwtAuthenticationFilter`):** Intercepta cada petición HTTP entrante, extrae el token del encabezado `Authorization: Bearer <token>`, valida la firma criptográfica y la fecha de expiración mediante `JwtService`, y establece la autenticación en el `SecurityContextHolder`.
*   **Contraseñas Seguras:** Las contraseñas se almacenan cifradas con **BCrypt** (`BCryptPasswordEncoder`) y están configuradas como `WRITE_ONLY` en las respuestas JSON para evitar fugas de información.
*   **Rutas Públicas vs Protegidas:**
    *   **Públicas (sin token):** `/api/auth/**` (login, registro), `/api/converter/**` (conversión PDF), Swagger UI (`/swagger-ui/**`, `/v3/api-docs/**`).
    *   **Protegidas (requieren token Bearer):** Endpoints de transacciones, perfiles y recomendaciones.

---

### 3.2. Motor de Clasificación Jerárquico (4 Niveles) y Aprendizaje Colaborativo

Cada transacción con descripción ingresada al sistema se procesa secuencialmente a través de un motor de clasificación (`CategoryClassifierService`):

1.  **Normalización de Texto (`TextNormalizer`):**
    *   Convierte el texto a mayúsculas.
    *   Remueve tildes y caracteres diacríticos (ej. `á` $\rightarrow$ `A`, `ñ` $\rightarrow$ `N`).
    *   Elimina números, RUTs, fechas, identificadores de sucursal y secuencias de operación variables.
    *   Colapsa espacios múltiples y aplica `.trim()`.
    *   *Ejemplo:* `"COMPRA JUMBO PROVIDENCIA 1234"` $\rightarrow$ `"COMPRA JUMBO PROVIDENCIA"`.

2.  **Cadena de Clasificación de 4 Niveles:**
    *   **Nivel 1 (Mapeo Colaborativo - BD):** Busca coincidencia exacta contra la tabla `transaction_category_mappings`. Si existe un patrón validado previamente por usuarios, asigna la categoría con nivel de confianza `1.0`.
    *   **Nivel 2 (Reglas por Palabras Clave - BD):** Busca si la descripción normalizada contiene alguna palabra clave registrada en la tabla `category_keywords` (ej. `"JUMBO"` $\rightarrow$ `FOOD`, `"METRO"` $\rightarrow$ `TRANSPORT`). Asigna la categoría con confianza `0.9`.
    *   **Nivel 3 (Modelo ML - Ciencia de Datos):** Si no hay coincidencias anteriores, invoca el servicio de inferencia `MlInferenceService` (modelo Scikit-Learn). Si el nivel de confianza reportado es $\ge 0.60$, asigna la categoría predicha.
    *   **Nivel 4 (Fallback por Defecto):** Si ningún nivel anterior resuelve la clasificación, asigna por defecto `OTHER_EXPENSE` (Confianza `0.0`).

3.  **Ciclo de Retroalimentación en Tiempo Real (`learnFromFeedback`):**
    *   Al ejecutar `PUT /api/transactions/{id}/category`, el usuario corrige la categoría sugerida.
    *   El sistema actualiza la transacción del usuario y ejecuta `learnFromFeedback()`, guardando o actualizando el patrón normalizado en `transaction_category_mappings` e incrementando su contador de frecuencia (`frequency`).
    *   Esta corrección beneficia inmediatamente a todos los usuarios, ya que futuras transacciones con la misma descripción se clasificarán instantáneamente en el **Nivel 1**.

---

## 4. Requisitos Específicos y Modelo de Datos

### 4.1 Modelo de Datos (Esquema Lógico)
El almacenamiento se estructura en las siguientes tablas en Oracle Autonomous Database / H2:

*   **`users`:** Almacena usuarios (`id`, `name`, `email`, `password` en hash BCrypt, `monthly_income`, `saving_frequency`, `financial_profile`, `profile_accuracy`, `profile_updated_at`).
*   **`transactions`:** Almacena los movimientos bancarios (`id`, `description`, `operation_number`, `amount`, `category`, `transaction_date`, `currency_id`, `balance_after`, `user_id`).
*   **`recommendations`:** Almacena los consejos generados (`id`, `text`, `generated_at`, `profile_at_generation`, `user_id`).
*   **`transaction_category_mappings`:** Almacena los patrones aprendidos por feedback (`id`, `description_pattern`, `category`, `frequency`).
*   **`category_keywords`:** Almacena el diccionario dinámico de palabras clave (`id`, `keyword`, `category`).
*   **Catálogos (Enums):**
    *   `financial_profile`: *SAVER*, *BALANCED*, *SPENDER*, *AT_RISK*.
    *   `saving_frequency`: *NEVER*, *RARELY*, *MONTHLY*, *BIWEEKLY*, *WEEKLY*, *DAILY*.
    *   `transaction_category`: *FOOD*, *TRANSPORT*, *HOUSING*, *UTILITIES*, *ENTERTAINMENT*, *HEALTH*, *EDUCATION*, *SHOPPING*, *SALARY*, *INVESTMENT*, *SAVINGS*, *OTHER_INCOME*, *OTHER_EXPENSE*.
    *   `transaction_type`: *INCOME*, *EXPENSE*, *SAVING*.

---

### 4.2 Requisitos Funcionales
*   **RF1 - Autenticación JWT:** Registro de usuarios, inicio de sesión seguro devolviendo token JWT y protección de endpoints privados.
*   **RF2 - Extracción de Cartolas:** Conversión de cartolas bancarias en formato PDF a formato Excel mediante Apache PDFBox y Apache POI.
*   **RF3 - Clasificación Inteligente:** Clasificar automáticamente transacciones mediante la cadena jerárquica de 4 niveles.
*   **RF4 - Retroalimentación y Aprendizaje:** Permitir la corrección de categorías (`PUT /api/transactions/{id}/category`) y actualizar el catálogo de patrones aprendidos en tiempo real.
*   **RF5 - Análisis y Perfil Financiero:** Determinar el perfil financiero del usuario y generar recomendaciones personalizadas.

---

### 4.3 Requisitos No Funcionales
*   **RNF1 - Seguridad Stateless y Cifrado:** Uso de Spring Security con JWT para autenticación sin sesión de servidor y contraseñas hasheadas con BCrypt.
*   **RNF2 - Escalabilidad de Reglas:** Almacenamiento de palabras clave en la tabla `category_keywords` de la base de datos en lugar de código Java hardcodeado, permitiendo gestión dinámica sin redesplegar.
*   **RNF3 - Disponibilidad en OCI:** Despliegue en arquitectura de 3 capas aislada en subredes privada y pública en Oracle Cloud Infrastructure.
*   **RNF4 - Documentación API:** Documentación completa de endpoints con Swagger UI en `/swagger-ui.html`.