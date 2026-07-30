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
| Base de datos | Oracle Autonomous Database — Transaction Processing (ATP), acceso con wallet |
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
curl.exe -X POST "http://localhost:8080/api/converter/pdf-to-excel" `
  -F "file=@..\Cartola CuentaRUT 20260523_000002.pdf;type=application/pdf" `
  -o salida.xlsx
---

## Despliegue

`.github/workflows/ci-cd.yml` construye las imágenes de `backend/` y `frontend/`, las publica en GHCR
en cada push a `main` y luego, en un runner *self-hosted*, levanta la nueva versión con Docker Compose.

### Configuración en el servidor

Las credenciales —contraseña de la base de datos y wallet de Oracle— **no viven en el repositorio**:
se dejan una sola vez en el servidor, en un directorio propio **fuera del workspace del runner**:

```
/opt/financeai/
├── .env                 # copia de .env.example, ya completada
└── secrets/wallet/      # wallet de Autonomous Database, descomprimido
    ├── cwallet.sso
    ├── ewallet.p12
    ├── tnsnames.ora
    └── sqlnet.ora
```

> **No poner el `.env` en el directorio donde el runner hace el checkout.** `actions/checkout` limpia
> el workspace con `git clean -ffdx`, y el flag `-x` incluye los archivos ignorados: el `.env` y el
> wallet se borrarían en cada despliegue.

Preparación, una sola vez en el servidor:

```bash
sudo mkdir -p /opt/financeai/secrets/wallet
sudo chown -R <usuario-del-runner> /opt/financeai

# copiar .env.example del repositorio a /opt/financeai/.env y completarlo,
# y descomprimir ahí el wallet descargado desde OCI
chmod 750 /opt/financeai
chmod 600 /opt/financeai/.env             # lo lee el CLI de Docker, como el usuario del runner
chmod 755 /opt/financeai/secrets/wallet   # lo lee el proceso dentro del contenedor
chmod 644 /opt/financeai/secrets/wallet/*
```

El wallet no puede quedar en `600`: el contenedor corre como el usuario `spring` (ver
`backend/Dockerfile`) y los *bind mounts* no traducen uids, así que un archivo de `ubuntu`
en modo `600` es ilegible dentro del contenedor y la conexión falla. La protección real la
da el directorio padre en `750`: ningún otro usuario del servidor puede entrar a
`/opt/financeai`, mientras que el montaje lo hace el demonio de Docker como root.

El job de despliegue copia el `docker-compose.yml` del repositorio a ese directorio y ejecuta Compose
desde allí, de modo que Compose carga el `.env` automáticamente (lo lee del directorio del proyecto)
y el montaje relativo `./secrets/wallet` resuelve a `/opt/financeai/secrets/wallet`. Los cambios en
`docker-compose.yml` se siguen desplegando normalmente, porque el archivo se copia desde el repo en
cada ejecución. Mientras `/opt/financeai` no exista, el despliegue continúa desde el workspace y deja
un *warning* en el log.

### Activar Oracle

Mientras `SPRING_PROFILES_ACTIVE` no sea `oracle`, el backend arranca con H2 en memoria y
**los datos se pierden en cada despliegue**. Para persistir en Autonomous Database:

1. Aprovisionar la instancia y crear el esquema — ver [backend/README.md → Base de datos](backend/README.md#base-de-datos).
2. Dejar el wallet descomprimido en `/opt/financeai/secrets/wallet/`.
3. En `/opt/financeai/.env`: `SPRING_PROFILES_ACTIVE=oracle`, `ORACLE_DB_PASSWORD=...` y el alias `_tp`
   correcto en `ORACLE_JDBC_URL`.
4. Volver a desplegar: push a `main`, o `docker compose up -d` desde `/opt/financeai`.

### Migración desde el workspace del runner

Si ya había contenedores levantados desde el directorio del checkout, hay que retirarlos una vez.
El nombre del proyecto de Compose viene del nombre del directorio, así que los contenedores antiguos
no se adoptan y chocan por `container_name`:

```bash
cd <workspace-antiguo> && docker compose down
# o, directamente: docker rm -f fintech-api fintech-frontend
```

### En local

Para levantar la misma pila en la máquina de desarrollo, el `.env` y `secrets/wallet/` van en la raíz
del repositorio (ambos están en `.gitignore`) y basta con `docker compose up -d`.

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
