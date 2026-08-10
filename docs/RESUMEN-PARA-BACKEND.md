# Finance AI — Resumen de la entrega de Data Science

**Para:** equipo de Backend
**De:** equipo de Data Science
**Cómo leer esto:** de punta a punta, en orden — está pensado para que no necesites abrir ningún otro documento para entender qué se hizo y por qué. Los detalles técnicos más finos (specs exactas de endpoints, DDL completo) están en otros `.md` de la carpeta `docs/`, pero acá está todo lo que hace falta para una primera lectura completa.

---

## 1. Qué es esto, en una frase

Dos entregas independientes: **código Java** listo para revisar y mergear (tres funcionalidades completas), y **un script Python + un contrato** para que ustedes construyan el endpoint de carga de cartolas cuando les quede tiempo. Todo lo que sigue explica el qué y el por qué de cada una.

---

## PARTE 1 — Java

### 1.1 Registro manual de transacciones

**El problema:** el banco nunca se entera de un gasto en efectivo. Sin una forma de registrar eso, el sistema tiene un punto ciego real sobre la situación financiera de la persona.

**Qué se agregó:**
- `POST /api/transactions/manual` — el usuario ingresa monto, categoría, medio de pago (efectivo o débito — tarjeta de crédito excluida del MVP a propósito, por la complejidad de su ciclo de facturación). La fecha la asigna el servidor, siempre "hoy".
- `PUT /api/transactions/{id}/category` — permite corregir una categoría sugerida automáticamente. Esta corrección es la base para mejorar la clasificación con el tiempo.
- Campos nuevos en `Transaction`: `source` (BANK/MANUAL — de dónde vino la transacción), `paymentMethod`, `linkStatus` y `linkedTransactionId` (para poder "conciliar" un registro manual con la transacción real de la cartola cuando aparezca, sin contar el mismo gasto dos veces), `categoryMethod` y `categoryConfidence` (cómo se determinó la categoría, y con qué confianza — necesario para saber qué datos son confiables a la hora de mejorar el clasificador).

**Por qué como columnas nuevas, y no una tabla aparte:** porque `transactions` ya es el lugar natural donde vive este dato — separar la información en otra tabla hubiera significado hacer un `JOIN` constante para algo que conceptualmente es parte de la misma transacción.

### 1.2 Motor de Recomendaciones Presupuestarias

**El problema:** el sistema podía clasificar un gasto, pero no tenía ninguna forma de decir si esa distribución de gasto era sana. El brief del hackathon pide explícitamente "identificar hábitos financieros de riesgo" y "presentar recomendaciones" — sin un punto de comparación, eso no es posible.

**Qué se agregó:**
- Tabla `category_budget_targets`: guarda, por categoría, qué porcentaje del gasto total es razonable. **Los valores no son inventados** — salen de la IX Encuesta de Presupuestos Familiares del INE (2022-2023), la encuesta real del gobierno de Chile sobre cómo gastan los hogares. Quedó con columna de país, para poder sumar otros mercados más adelante solo insertando filas.
- `BudgetRecommendationService`: compara el gasto real de un usuario contra esos porcentajes, y genera recomendaciones cuando hay desbalance (ej. "gastás 22% en entretenimiento, lo recomendado es 5%"). Con 3 niveles de severidad, chequeo de tasa de ahorro aparte, un tope de recomendaciones por corrida (para no saturar al usuario), y un enfriamiento de 7 días antes de repetir la misma alerta.
- `POST /api/recommendations/generate` — el endpoint que dispara este análisis.

**Por qué sin Machine Learning:** comparar un gasto contra un presupuesto de referencia es una resta y una división, no algo que necesite un modelo entrenado. Usar ML ahí hubiera sido más lento, más difícil de explicar, y no habría aportado nada mejor — se guardó la complejidad de ML para donde sí hace falta (clasificar texto libre, ver Parte 2).

**Un detalle importante para Backend:** las transferencias entre cuentas (`TRANSF`, `TEF`, `GIRO`) se excluyen del cálculo de este motor — se detectan por texto en la descripción, no por una categoría nueva. Esto fue una decisión deliberada para no tener que agregar categorías a `Category.java`: el dinero se sigue reflejando en la base de datos, solo no se cuenta como "gasto de consumo" al calcular porcentajes.

### 1.3 Historial de perfil financiero

**El problema:** `users.financial_profile` se sobreescribe cada vez que se actualiza. Eso significa que si alguien mejora de "en riesgo" a "saludable" en 3 meses, hoy **no hay forma de mostrar esa evolución** — solo se ve el estado actual, nunca la trayectoria. Es un requisito explícito del brief ("seguimiento de la evolución del comportamiento financiero en el tiempo").

**Qué se agregó:**
- Tabla `financial_profile_history`, enlazada por FK a `users`. Una fila nueva cada vez que se actualiza el perfil, sin tocar `users` en absoluto.
- Un único hook agregado en `UserService.updateProfile()` — todo el resto del método sigue exactamente igual.

**Para qué sirve, más allá de mostrar un gráfico:** es la base necesaria del "análisis evolutivo del cliente" que se definió como objetivo — comparar el gasto actual de un usuario contra su **propio promedio histórico** (no contra el porcentaje fijo del INE), usando estadística simple. Sin esta tabla, no hay datos sobre los cuales calcular ese promedio, sin importar qué tan bien se programe la lógica.

### 1.4 Qué NO se tocó, a propósito

- **`Category.java` / `TransactionType.java` sin cambios** — todo lo anterior se adapta al enum real tal como está, no le pide modificaciones a Backend.
- **Ningún endpoint ni comportamiento existente cambia** — todo lo agregado es aditivo.
- **Frontend no se toca.**

---

## PARTE 2 — Python (Data Science)

### 2.1 El problema que se venía arrastrando

Ningún banco entrega la categoría de una transacción — eso hay que inferirlo del texto de la descripción. Además, cada banco tiene su propio formato (columnas distintas, fechas en español, montos separados en cargo/abono en vez de un valor con signo). Se necesitaba un proceso que leyera cualquiera de estos formatos y devolviera datos limpios y clasificados.

### 2.2 Qué se construyó

- **`analisis_cartola.ipynb`** — el notebook de análisis: lee `.xlsx`, `.xls`, `.csv` y `.pdf`, detecta automáticamente banco/país/año, limpia texto y montos, y clasifica cada transacción usando **las mismas 13 categorías reales de `Category.java`** (a propósito — no se inventó una taxonomía propia). Probado contra cartolas reales de 4 bancos chilenos (Banco Chile, CuentaRUT, Falabella, Mercado Pago).
- **`procesar_cartola_cli.py`** — el mismo pipeline exacto del notebook, convertido a un script de línea de comandos, para que se pueda invocar desde código (no depende de Jupyter). Recibe la ruta de un archivo y devuelve un único JSON por `stdout`, con código de salida `0` (éxito) o `1` (error) — nunca un traceback crudo.

### 2.3 Por qué se entrega como contrato, y no como código Java

Se evaluaron 3 caminos: reescribir toda esta lógica en Java, correr un servicio Python separado que escriba directo a la base de datos, o que Backend invoque el script Python como subproceso desde un endpoint nuevo. Se descartaron los primeros dos:

- **Reescribir en Java** significa reescribir algo que ya funciona y está probado, sin necesidad real.
- **Un servicio Python aparte escribiendo directo a Oracle** duplicaría la lógica de negocio que ya vive en `TransactionService` (validación de usuario, resolución de moneda, etc.), necesitaría sus propias credenciales de base de datos, y — el problema de fondo — **el frontend nunca recibiría respuesta**, porque no habría ningún ciclo de petición/respuesta hacia el usuario.

Por eso la entrega es: **nosotros el script probado + el contrato exacto de cómo integrarlo; Backend construye el endpoint que lo invoca y escribe el resultado usando el `TransactionService` que ya existe.** Así hay una sola fuente de verdad escribiendo en la base, sin duplicar lógica ni credenciales.

### 2.4 Qué necesita Backend para integrarlo (resumen — el contrato completo tiene más detalle)

- El endpoint recibe el archivo, lo guarda temporalmente en disco, invoca `python3 procesar_cartola_cli.py <ruta>` como subproceso, parsea el JSON de salida, y crea las `Transaction` correspondientes con `source = BANK`.
- El contenedor del backend necesita Python 3.11+ y estas librerías: `pandas`, `openpyxl`, `xlrd`, `pdfplumber`, `holidays` (esta última es opcional).
- Un detalle que conviene no pasar por alto: el script devuelve el monto **con signo** (negativo si es un cargo); `Transaction.amount` es siempre positivo — hay que tomar el valor absoluto al mapear.

### 2.5 Qué NO se construyó en esta parte, a propósito

Ni el endpoint, ni el cambio al `Dockerfile`, ni la escritura en base de datos — eso es responsabilidad de Backend, con el contrato ya cerrado para que no haya ambigüedad de qué se espera.

---

## 3. Lo que sigue pendiente (para que quede sin sorpresas)

- **El modelo de Machine Learning para clasificar texto libre todavía no está entrenado.** Es el único entregable obligatorio del brief que falta construir — todo lo demás (reglas, mapeos, mock del frontend) es complementario a esto, no un reemplazo.
- **El plan de clasificación híbrido de 4 niveles** (mapeo aprendido → reglas → modelo → fallback), diseñado en conjunto con un compañero de Backend, **no está implementado en código todavía**, ni la parte de reglas/mapeo (Backend) ni la del modelo (Data Science). No bloquea nada de lo entregado acá — el Motor de Recomendaciones Presupuestarias solo lee la categoría ya asignada, sin importar cómo se resolvió.
- **El job que concilia un registro manual con la transacción real de la cartola** (mismo monto, ventana de 2-3 días) tiene el modelo de datos listo (`linkStatus`, `linkedTransactionId`) pero el proceso en sí todavía no está escrito.

---

## 4. Validación pendiente antes de mergear

El código Java se revisó a mano con cuidado (referencias cruzadas, imports, coherencia con las entidades reales), pero no se pudo compilar con Maven real — sin salida a Maven Central en el entorno de trabajo. Antes de mergear con total confianza:

```bash
cd backend
./mvnw compile
./mvnw test
```

El script Python sí se probó de punta a punta, con archivos reales de los 4 bancos.

---

*Para el detalle técnico completo (DDL exacto, specs de cada endpoint, diagramas), ver `docs/documentacion-tecnica.md` y `docs/contrato-ingesta-cartolas.md` en la misma entrega.*
