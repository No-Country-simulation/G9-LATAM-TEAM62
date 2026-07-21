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
| Persistencia | En memoria (`ConcurrentHashMap`) — **provisional, se reinicia con la app** |

## Ejecución

Desde la carpeta `backend/`:

```bash
./mvnw spring-boot:run      # inicia la API en http://localhost:8080
./mvnw test                 # ejecuta la suite de pruebas
./mvnw clean package        # genera un jar ejecutable en target/
```

No se necesita base de datos ni servicios externos para ejecutarla: el almacenamiento es en memoria.

## Arquitectura

Diseño REST en capas estándar, sin capa de vista:

```
Controller  ->  Service  ->  Repository  ->  (almacén en memoria)
   HTTP       lógica de negocio  acceso a datos
```

- **Controllers**: manejan solo HTTP, validación (`@Valid`) y códigos de estado.
- **Services**: contienen las reglas de negocio (integridad referencial, borrado en cascada, hash de contraseñas, y el límite del perfil que gestiona la app externa).
- **Repositories**: almacenes en memoria (`ConcurrentHashMap`) con un generador de ids`AtomicLong`. Son un reemplazo provisional a propósito: como los controllers y services solo dependen de las firmas de los métodos del repositorio, migrar más adelante a Spring Data JPA es un cambio que no rompe nada.

Estructura de paquetes bajo `com.g9latam.team62.fintech_api`:

```
controller/   endpoints REST + GlobalExceptionHandler
service/      lógica de negocio
repository/   almacenes de datos en memoria
model/        objetos de dominio y enums
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
| `date` | Date | obligatorio, pasado o presente |
| `balanceAfter` | BigDecimal | saldo de la cuenta después de esta transacción |
| `userId` | Long | obligatorio; debe referenciar a un usuario existente |

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
| `400 Bad Request` | Falla de validación (Bean Validation), o un `userId` referenciado no existe |
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

# Registrar una transacción
curl -X POST http://localhost:8080/api/transactions \
  -H 'Content-Type: application/json' \
  -d '{"amount":42.50,"category":"FOOD","date":"2026-07-20","userId":1}'

# La app externa escribe el perfil calculado
curl -X PUT http://localhost:8080/api/users/1/profile \
  -H 'Content-Type: application/json' \
  -d '{"financialProfile":"SAVER","profileAccuracy":0.87}'

# Listar las recomendaciones de un usuario
curl http://localhost:8080/api/recommendations?userId=1
```

## Limitaciones actuales

Son decisiones de alcance deliberadas para el MVP, no errores:

- **Almacenamiento en memoria** — todos los datos se pierden al reiniciar. Los repositorios están estructurados para poder incorporar una capa de persistencia Spring Data JPA sin tocar los controllers ni los services.
- **Sin control de acceso** — `login` verifica credenciales pero no emite token ni sesión, por lo que actualmente todos los endpoints están abiertos. Introducir JWT / Spring Security es el siguiente paso natural cuando el frontend esté listo para enviar cabeceras `Authorization`.
- **El cálculo del perfil es externo** — esta API solo almacena perfiles; no los calcula.
