# Servicio de Clasificación ML (Nivel 3) — cómo conectarlo

**De:** equipo de Data Science
**Para:** equipo de Backend
**A diferencia del script de ingesta de cartolas:** esto NO corre dentro del contenedor de Backend — es un servicio aparte, con su propio `Dockerfile`. Solo hace falta apuntarle la URL.

---

## 1. Qué es

`ml-model/` — un servicio FastAPI que expone `POST /predict`, exactamente el contrato que ya describieron ustedes mismos en `docs/backend/README_DATA_SCIENCE_BACKEND .md`:

```json
// Request
{"descripcion": "COMPRA JUMBO PROVIDENCIA"}
// Response
{"category": "FOOD", "confidence": 0.93}
```

Reporte de evaluación completo (métricas, matriz de confusión, limitaciones conocidas) en `ml-model/reporte-evaluacion-modelo.md`.

## 2. Cómo levantarlo

**Con Docker (recomendado, mismo patrón que ya usan):**

```bash
cd ml-model
docker build -t finance-ai-ml .
docker run -p 8000:8000 finance-ai-ml
```

**O agregarlo a `docker-compose.yml`** como un servicio más (no lo tocamos nosotros, para no pisar su archivo — este es el bloque a sumar):

```yaml
  ml-service:
    build: ./ml-model
    ports:
      - "8000:8000"
```

## 3. Cómo activarlo del lado de Backend

Un solo cambio en `application.properties` (o `application-oracle.properties` para producción):

```properties
ml.inference.enabled=true
ml.inference.url=http://ml-service:8000/predict
```

**Ojo con la URL:** si lo corren dentro del mismo `docker-compose.yml`, el nombre del servicio (`ml-service` en el ejemplo de arriba) reemplaza a `localhost` — así se resuelven entre contenedores en la misma red de Docker. Si lo prueban suelto en su máquina antes de eso, `http://localhost:8000/predict` funciona igual.

## 4. Verificación rápida

```bash
curl http://localhost:8000/health
# {"status":"ok","model_loaded":true}

curl -X POST http://localhost:8000/predict \
  -H "Content-Type: application/json" \
  -d '{"descripcion": "COMPRA JUMBO PROVIDENCIA"}'
# {"category":"FOOD","confidence":0.93}
```

Con esto activo, el Nivel 3 de `CategoryClassifierService` deja de estar deshabilitado — el pipeline de 4 niveles queda completo por primera vez.

## 5. Qué NO incluye esta entrega, a propósito

- No tocamos `docker-compose.yml` ni `application.properties` reales — son archivos de infraestructura de ustedes, les dejamos el bloque exacto para que lo agreguen.
- No incluye el "evolutivo del cliente" (comparación contra el promedio histórico del propio usuario) — sigue en evaluación; falta la agregación mensual por categoría y la comparación estadística, más confirmar que haya suficiente historial real cargado por usuario para que tenga sentido.
