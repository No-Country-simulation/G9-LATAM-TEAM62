# Probar el backend localmente — inicio rápido

**Esto alcanza para probar ya, sin credenciales de Oracle.** El perfil `dev` usa H2 en memoria — se crea y se destruye solo, no necesita nada externo.

```bash
cd backend
./mvnw spring-boot:run
```

Por defecto corre en `http://localhost:8080` con el perfil `dev` (`spring.profiles.default=dev`). La consola de H2 queda disponible en `http://localhost:8080/h2-console` para inspeccionar las tablas mientras la app corre.

**Con Docker Compose, igual de simple** (usa las imágenes ya publicadas en GHCR, no requiere compilar nada):

```bash
cp .env.example .env   # dejar SPRING_PROFILES_ACTIVE=dev
docker compose up
```

Para probar contra Oracle real (perfil `oracle`), hace falta el wallet descargado desde la consola de OCI y las credenciales reales — eso lo vemos con calma en la próxima conversación, junto con la conexión que pediste en el punto 6.
