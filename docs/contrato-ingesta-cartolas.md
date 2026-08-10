# Ingesta de cartolas — requisitos para Backend

**De:** equipo de Data Science
**Para:** equipo de Backend
**Qué es esto:** el contrato exacto que necesitan para construir el endpoint de carga de cartolas, invocando nuestro script Python. Nosotros entregamos el script y este documento; la implementación del endpoint, el `Dockerfile`, y la escritura en base de datos son responsabilidad de Backend.

---

## 1. Qué entregamos nosotros

**`data-science/procesar_cartola_cli.py`** — un script de línea de comandos, sin dependencias de Jupyter, que:
- Recibe la ruta de un archivo de cartola (`.xlsx`, `.xls`, `.csv`, o `.pdf`)
- Detecta banco, país y año automáticamente
- Limpia y normaliza el texto, los montos y las fechas
- Clasifica cada transacción usando las 13 categorías reales de `Category.java` — no genera categorías inventadas
- Devuelve **un único JSON por stdout**, listo para insertar

Probado de punta a punta contra archivos reales de los 4 bancos (Banco Chile, CuentaRUT, Falabella, Mercado Pago) — no es teórico, ya corre.

---

## 2. Cómo invocarlo

```bash
python3 procesar_cartola_cli.py <ruta_al_archivo> [--anio-defecto 2026] [--pais CL]
```

Desde Java, con `ProcessBuilder`:

```java
ProcessBuilder pb = new ProcessBuilder("python3", "procesar_cartola_cli.py", rutaArchivoTemporal.toString());
pb.redirectErrorStream(false); // stdout = JSON de resultado, stderr = solo para debug nuestro
Process proceso = pb.start();

String salidaJson = new String(proceso.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
boolean terminoATiempo = proceso.waitFor(30, TimeUnit.SECONDS); // ver recomendación de timeout abajo
int codigoSalida = proceso.exitValue();
```

**Importante:** el archivo que suba el usuario (`MultipartFile`) hay que guardarlo primero como archivo temporal en disco — el script necesita una ruta, no puede leer el multipart directamente. Backend es responsable de crear y borrar ese archivo temporal (idealmente en un `try/finally`, para no dejar basura si algo falla a mitad de camino).

---

## 3. Contrato de salida (éxito)

Un único objeto JSON por stdout, siempre en una sola línea:

```json
{
  "status": "ok",
  "archivo": "Cartola_bancoChile.xlsx",
  "pais": "CL",
  "anio": 2026,
  "filas_crudas": 39,
  "filas_validas": 36,
  "filas_descartadas": 3,
  "avisos": [],
  "transacciones": [
    {
      "fecha": "2026-02-02",
      "descripcion": "PAGO DLP",
      "categoria": "OTHER_EXPENSE",
      "tipo_movimiento": "cargo",
      "monto": -1900.0,
      "saldo": 32852.0,
      "feriado": false
    }
  ]
}
```

**Código de salida: `0`.**

### Mapeo directo a `Transaction` (para que quede sin ambigüedad)

| Campo del JSON | Campo de `Transaction` | Nota |
|---|---|---|
| `descripcion` | `description` | — |
| `categoria` | `category` | Ya es uno de los 13 valores válidos del enum — no necesita traducción |
| `monto` (con signo) | `amount` | **Ojo:** el script devuelve el monto con signo (negativo si es cargo). `Transaction.amount` es siempre positivo (`@Positive`) — Backend debe tomar el valor absoluto al mapear |
| `fecha` | `date` | Formato `YYYY-MM-DD`, parseable directo con `LocalDate.parse()` |
| `saldo` | `balanceAfter` | Puede venir `null` (ver Mercado Pago en las pruebas — ese formato no expone saldo por movimiento) |
| `tipo_movimiento` | *(no tiene columna directa)* | `"cargo"`/`"abono"` — útil para decidir el signo, no para guardar tal cual |
| `feriado` | *(no aplica a `Transaction`)* | Es metadata del análisis, no del dato bancario — no hace falta persistirla |

Además, cada `Transaction` creada desde este flujo debe llevar `source = BANK` (ya existe ese campo, ver `docs/documentacion-tecnica.md` sección 3.3).

---

## 4. Contrato de salida (error)

También un único JSON por stdout, nunca un stack trace crudo:

```json
{
  "status": "error",
  "archivo": "archivo.xlsx",
  "mensaje": "No se pudo leer el archivo: [Errno 2] No such file or directory: 'archivo.xlsx'"
}
```

**Código de salida: `1`.** Backend debería devolver un `400` o `422` al frontend con el `mensaje` tal cual (ya viene en español, listo para mostrar).

---

## 5. Qué necesita el `Dockerfile` del backend

Hoy la imagen del backend solo tiene JVM. Para que esto funcione en producción, necesita además:

- **Python 3.11+**
- Estas librerías (`pip install`, todas puras Python o con wheels precompilados, sin necesidad de compilador C):
  ```
  pandas
  openpyxl      # lectura de .xlsx
  xlrd          # lectura de .xls antiguos
  pdfplumber    # lectura de .pdf
  holidays      # detección de feriados (opcional -- el script sigue funcionando sin ella, con "feriado": false para todo)
  ```
- El script (`procesar_cartola_cli.py`) copiado a una ruta conocida dentro de la imagen

No hace falta Jupyter, ni ningún paquete de visualización — el script no genera gráficos.

---

## 6. Recomendaciones operativas (para que no se caiga en producción)

- **Timeout:** 30 segundos es más que suficiente para cualquier cartola real que probamos (la más grande tardó menos de 2 segundos). Si el proceso no termina en ese plazo, matarlo y devolver error — mejor eso que dejar una request colgada.
- **Tamaño máximo de archivo:** sugerimos limitar el upload a algo como 10 MB antes de siquiera invocar el script — ninguna cartola real debería acercarse a eso.
- **Un proceso por request, sin pool de procesos Python persistente.** Cada invocación es corta y stateless — no vale la pena la complejidad de mantener un proceso Python vivo escuchando.
- **El archivo temporal se borra siempre**, incluso si el script falla — si no, con el tiempo el disco del contenedor se llena de cartolas de prueba.

---

## 7. Qué NO es responsabilidad nuestra en esta entrega

Para que quede explícito y no haya ambigüedad de alcance:

- El endpoint REST en sí (`POST /api/transactions/import` o el nombre que Backend prefiera)
- La invocación del subproceso desde Java
- El cambio al `Dockerfile`
- La escritura final en la base de datos (reutilizando `TransactionService.create()`, que ya existe)
- La respuesta que ve el usuario en el frontend

Todo eso es del lado de Backend. Nosotros entregamos el script probado y este contrato — no vamos a tocar código Java para esta funcionalidad.
