# Finance AI — API Backend

API REST para **Finance AI**, una aplicación que entrega recomendaciones financieras a los usuarios según su comportamiento de gasto y ahorro. Este servicio almacena usuarios, sus transacciones y las recomendaciones generadas para ellos.

El **cálculo del perfil financiero lo realiza una aplicación externa** independiente, que escribe sus resultados en esta API a través de un endpoint dedicado (`PUT /api/users/{id}/profile`). Este backend no calcula perfiles por sí mismo: es dueño de los datos y de la superficie de la API.

## Tecnologías

| | |
|---|---|
| Lenguaje | Java 25 (Eclipse Temurin) |
| Framework | Spring Boot 4.1.0 (Spring MVC) |
| Build | Maven (mediante el wrapper incluido `./mvnw`) |
| Validación | Jakarta Bean Validation (`spring-boot-starter-validation`) |
| Contraseñas | Hash con BCrypt (`spring-security-crypto`) |
| Persistencia | Spring Data JPA (Hibernate) |
| Base de datos | Oracle Autonomous Database — ATP (perfil `oracle`); H2 en memoria para desarrollo (perfil `dev`) |

## Ejecución

Desde la carpeta `backend/`:

```bash
./mvnw spring-boot:run      # inicia la API en http://localhost:8080 (perfil dev, H2 en memoria)
./mvnw test                 # ejecuta la suite de pruebas
./mvnw clean package        # genera un jar ejecutable en target/
```

Para desarrollo local no se necesita ninguna base de datos: el perfil por defecto (`dev`)
levanta un H2 en memoria, crea el esquema desde las entidades y siembra las monedas.
Consola H2 en `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:fintech`, usuario `sa`, sin contraseña).

Contra Oracle se activa el perfil `oracle` (ver [Base de datos](#base-de-datos)):

```bash
SPRING_PROFILES_ACTIVE=oracle \
ORACLE_JDBC_URL='jdbc:oracle:thin:@financeai_tp?TNS_ADMIN=/ruta/al/wallet' \
ORACLE_DB_USER=FINTECH_APP ORACLE_DB_PASSWORD='...' \
./mvnw spring-boot:run
```

## Arquitectura

Diseño REST en capas estándar, sin capa de vista:

```
Controller  ->  Service  ->  Repository  ->  Oracle / H2
   HTTP       lógica de negocio  Spring Data JPA
```

- **Controllers**: manejan solo HTTP, validación (`@Valid`) y códigos de estado.
- **Services**: contienen las reglas de negocio (integridad referencial, borrado en cascada, hash de contraseñas, resolución de la moneda, y el límite del perfil que gestiona la app externa). Definen además los límites transaccionales (`@Transactional`).
- **Repositories**: interfaces `JpaRepository`. Los ids los genera la base de datos con columnas *identity*.

Estructura de paquetes bajo `com.g9latam.team62.fintech_api`:

```
controller/   endpoints REST + GlobalExceptionHandler
service/      lógica de negocio y transacciones
repository/   interfaces Spring Data JPA
model/        entidades JPA y enums
dto/          payloads solo de entrada (LoginRequest, ProfileUpdateRequest)
```

## Modelo de datos

### Usuario
| Campo | Tipo | Notas |
|---|---|---|
| `id` | Long | asignado por el servidor |
| `name` | String | obligatorio |
| `email` | String | obligatorio, email válido, único — es el identificador de login |
| `password` | String | obligatorio; se guarda como hash BCrypt, **nunca se devuelve** en las respuestas |
| `monthlyIncome` | BigDecimal | ≥ 0 |
| `savingFrequency` | enum | `NEVER, RARELY, MONTHLY, BIWEEKLY, WEEKLY, DAILY` |
| `financialProfile` | enum | `SAVER, BALANCED, SPENDER, AT_RISK` — **lo escribe solo la app externa** |
| `profileAccuracy` | Double | 0.0–1.0, confianza del perfil — **solo la app externa** |
| `profileUpdatedAt` | DateTime | cuándo se calculó el perfil por última vez — **solo la app externa** |

Los tres campos de perfil solo pueden establecerse mediante `PUT /api/users/{id}/profile`.
Se ignoran en `POST`/`PUT /api/users/{id}` aunque un cliente los incluya, de modo que los datos de perfil no pueden ser falsificados por un cliente común.

### Transacción
| Campo | Tipo | Notas |
|---|---|---|
| `id` | Long | asignado por el servidor |
| `description` | String | opcional |
| `operationNumber` | String | referencia opcional de la operación bancaria |
| `amount` | BigDecimal | obligatorio, positivo — la dirección la da la categoría, no el signo |
| `category` | enum | obligatorio (ver abajo) |
| `type` | enum | **derivado** de la categoría (`INCOME`, `EXPENSE`, `SAVING`) — solo lectura |
| `date` | Date | obligatorio, pasado o presente (columna `transaction_date`: `date` es palabra reservada en Oracle) |
| `currency` | objeto | obligatorio; referencia a una moneda ya registrada — `{"id": 1}` o `{"name_currency": "CLP"}` |
| `balanceAfter` | BigDecimal | saldo de la cuenta después de esta transacción |
| `userId` | Long | obligatorio; debe referenciar a un usuario existente |

La moneda no se crea sobre la marcha: se resuelve contra la tabla `currencies`
(por id, o por nombre sin distinguir mayúsculas). Una moneda desconocida devuelve `400`.
El seed inicial trae `CLP`, `USD` y `EUR`.

`Category` lleva su propio `TransactionType`, así que `type` se deriva en lugar de almacenarse — esto hace imposible una contradicción entre categoría y tipo:

```
FOOD, TRANSPORT, HOUSING, UTILITIES, ENTERTAINMENT, HEALTH, EDUCATION, SHOPPING, OTHER_EXPENSE          -> EXPENSE
SALARY, OTHER_INCOME                        -> INCOME
INVESTMENT, SAVINGS                         -> SAVING
```

### Recomendación
| Campo | Tipo | Notas |
|---|---|---|
| `id` | Long | asignado por el servidor |
| `text` | String | obligatorio |
| `generatedAt` | DateTime | lo marca el servidor al crearla |
| `profileAtGeneration` | enum | el perfil que tenía el usuario cuando se generó |
| `userId` | Long | obligatorio; debe referenciar a un usuario existente |

### Moneda
| Campo | Tipo | Notas |
|---|---|---|
| `id` | Long | asignado por el servidor |
| `name_currency` | String | obligatorio, único (`CLP`, `USD`, `EUR`, …) |

Las monedas son datos de referencia: se cargan con `db/oracle/data.sql` y no tienen endpoints propios.

## Base de datos

### Qué tipo de Autonomous Database usar

**Autonomous Transaction Processing (ATP), Serverless, Always Free.**

La carga de esta API es OLTP pura: muchas operaciones cortas de una fila
(`POST /api/users`, `POST /api/transactions`, login, lecturas por `userId`),
integridad referencial con claves foráneas y volúmenes pequeños. Eso es
exactamente para lo que está afinada ATP (índices B-tree, row store).

Las otras opciones no encajan:

| Tipo | Por qué no |
|---|---|
| **ADW** (Data Warehouse) | Optimizado para escaneos analíticos y consultas paralelas sobre tablas grandes; penaliza los `INSERT`/`UPDATE` de una fila que domina esta API. Sería la opción correcta solo si la parte de Ciencia de Datos llegara a necesitar su propio almacén analítico — puede convivir como una segunda instancia. |
| **AJD** (JSON Database) | Pensada para documentos sin esquema; aquí hay un modelo relacional con FKs, `UNIQUE` y `CHECK` que ya se aprovecha. |
| **APEX** | Para aplicaciones construidas dentro de APEX; el frontend es React + Vite y el backend Spring Boot. |

Detalles del aprovisionamiento:

- **Serverless** (no *Dedicated*): sin infraestructura que administrar y es lo que cubre el tier gratuito.
- **Always Free**: 2 OCPU / 20 GB, suficiente de sobra para el hackathon y sin costo. Si se migra a un tier de pago, ECPU con *auto scaling*.
- **Versión 23ai** o 19c: ambas sirven; el driver y las entidades no cambian.
- Servicio de conexión: **`_tp`** (transaction processing) para esta API. `_tpurgent` se reserva para trabajos urgentes y `_low`/`_high` son perfiles de ADW.
- Acceso: **mTLS con wallet** (el modo por defecto), que es lo que espera la configuración de abajo.

### Puesta en marcha

1. En OCI: *Oracle Database → Autonomous Database → Create*, tipo **Transaction Processing**, *Always Free*.
2. Descargar el wallet (*DB Connection → Download Wallet*) y descomprimirlo en `secrets/wallet/`
   en la raíz del repositorio (está en `.gitignore`; **el wallet es una credencial, nunca se commitea**).
3. Crear un usuario de aplicación — no usar `ADMIN` para la app. Desde *Database Actions → SQL* como `ADMIN`:

   ```sql
   CREATE USER fintech_app IDENTIFIED BY "<contraseña>";
   GRANT CONNECT, RESOURCE TO fintech_app;
   ALTER USER fintech_app QUOTA UNLIMITED ON DATA;
   ```

4. Crear el esquema conectado **como `fintech_app`**, ejecutando en orden:

   ```
   src/main/resources/db/oracle/schema.sql   -- tablas, índices y constraints
   src/main/resources/db/oracle/data.sql     -- monedas iniciales
   ```

   `db/oracle/drop.sql` deshace todo si hace falta empezar de nuevo.

5. Configurar las variables de entorno y arrancar con el perfil `oracle`:

   | Variable | Ejemplo |
   |---|---|
   | `SPRING_PROFILES_ACTIVE` | `oracle` |
   | `ORACLE_JDBC_URL` | `jdbc:oracle:thin:@financeai_tp?TNS_ADMIN=/app/wallet` |
   | `ORACLE_DB_USER` | `FINTECH_APP` |
   | `ORACLE_DB_PASSWORD` | la contraseña del paso 3 |

   `financeai_tp` es un alias del `tnsnames.ora` que viene dentro del wallet.
   En Docker el wallet se monta en `/app/wallet` (ya configurado en `docker-compose.yml`
   y en la variable `TNS_ADMIN` del `Dockerfile`); en local, `TNS_ADMIN` apunta a la carpeta descomprimida.

Con Compose en local basta copiar `.env.example` a `.env` en la raíz del repositorio,
completarlo y dejar el wallet descomprimido en `secrets/wallet/`. En el servidor, el `.env` y el
wallet viven en el directorio de despliegue (`/opt/financeai/`) y **no** en el workspace del runner
—que se limpia en cada despliegue—: ver [Despliegue](../README.md#despliegue).

### Esquema y migraciones

El esquema de Oracle lo define `db/oracle/schema.sql` y se aplica a mano. La aplicación
arranca con `spring.jpa.hibernate.ddl-auto=validate`: no modifica la base de datos, pero
**se niega a arrancar si las entidades y las tablas no coinciden**. Si se cambia una
entidad, hay que actualizar el script en el mismo commit (y aplicar el `ALTER TABLE`
correspondiente en la instancia).

En el perfil `dev` es al revés: Hibernate crea el esquema desde las entidades
(`create-drop`) y `data.sql` siembra las monedas, así que los tests y el desarrollo
local no dependen de Oracle.

## Endpoints

URL base: `http://localhost:8080`

### Autenticación
| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/auth/login` | Verifica credenciales. Body: `{ "email", "password" }`. Devuelve el usuario (sin contraseña) si es correcto, o `401` si falla. |

Una contraseña incorrecta y un email desconocido devuelven la **misma** respuesta `401`, así la API no revela qué emails están registrados. El login solo verifica credenciales: todavía no emite un token ni una sesión, por lo que los endpoints no están protegidos (ver [Limitaciones conocidas](#limitaciones-conocidas)).

### Usuarios
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/users` | Lista todos los usuarios |
| GET | `/api/users/{id}` | Obtiene un usuario (`404` si no existe) |
| POST | `/api/users` | Crea un usuario (`201`). La contraseña se hashea; los campos de perfil se limpian |
| PUT | `/api/users/{id}` | Actualiza un usuario (`404` si no existe). Los campos de perfil se conservan, no se sobrescriben |
| PUT | `/api/users/{id}/profile` | **Solo app externa.** Establece `financialProfile` + `profileAccuracy` (+ opcional `savingFrequency`) y marca `profileUpdatedAt` |
| DELETE | `/api/users/{id}` | Elimina un usuario (`204`). **En cascada**: también elimina sus transacciones y recomendaciones |

### Transacciones
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/transactions` | Lista todas, o filtra con `?userId={id}` |
| GET | `/api/transactions/{id}` | Obtiene una (`404` si no existe) |
| POST | `/api/transactions` | Crea (`201`). `userId` debe referenciar a un usuario existente, si no `400` |
| PUT | `/api/transactions/{id}` | Actualiza (`404` si no existe) |
| DELETE | `/api/transactions/{id}` | Elimina (`204`, o `404` si no existe) |

### Recomendaciones
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/recommendations` | Lista todas, o filtra con `?userId={id}` |
| GET | `/api/recommendations/{id}` | Obtiene una (`404` si no existe) |
| POST | `/api/recommendations` | Crea (`201`). `userId` debe existir; `generatedAt` y `profileAtGeneration` los establece el servidor |
| DELETE | `/api/recommendations/{id}` | Elimina (`204`, o `404` si no existe) |

No existe `PUT` para recomendaciones por diseño: son artefactos generados, no editables.

## Respuestas de error

Los errores devuelven un body JSON `{ "error": "<mensaje>" }`, mapeado de forma
centralizada en `GlobalExceptionHandler`:

| Estado | Cuándo |
|---|---|
| `400 Bad Request` | Falla de validación (Bean Validation), o un `userId`/`currency` referenciado no existe |
| `401 Unauthorized` | Login fallido |
| `404 Not Found` | El id del recurso no existe |
| `409 Conflict` | El email ya está registrado |

## Ejemplos de peticiones

```bash
# Crear un usuario
curl -X POST http://localhost:8080/api/users \
  -H 'Content-Type: application/json' \
  -d '{"name":"Ada","email":"ada@example.com","password":"secret","monthlyIncome":3000}'

# Iniciar sesión
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"ada@example.com","password":"secret"}'

# Registrar una transacción (la moneda va por id o por nombre)
curl -X POST http://localhost:8080/api/transactions \
  -H 'Content-Type: application/json' \
  -d '{"amount":42.50,"category":"FOOD","date":"2026-07-20","currency":{"name_currency":"CLP"},"userId":1}'

# La app externa escribe el perfil calculado
curl -X PUT http://localhost:8080/api/users/1/profile \
  -H 'Content-Type: application/json' \
  -d '{"financialProfile":"SAVER","profileAccuracy":0.87}'

# Listar las recomendaciones de un usuario
curl http://localhost:8080/api/recommendations?userId=1
```

## Limitaciones actuales

Son decisiones de alcance deliberadas para el MVP, no errores:

- **Esquema aplicado a mano** — no hay herramienta de migraciones (Flyway/Liquibase). `db/oracle/schema.sql` se ejecuta una vez y los cambios posteriores se aplican con `ALTER TABLE`; `ddl-auto=validate` avisa al arrancar si el esquema y las entidades se desincronizan.
- **Sin control de acceso** — `login` verifica credenciales pero no emite token ni sesión, por lo que actualmente todos los endpoints están abiertos. Introducir JWT / Spring Security es el siguiente paso natural cuando el frontend esté listo para enviar cabeceras `Authorization`.
- **El cálculo del perfil es externo** — esta API solo almacena perfiles; no los calcula.
