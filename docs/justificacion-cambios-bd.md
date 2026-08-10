# Por qué estos cambios de base de datos — justificación para el equipo

**Para:** Backend y Ciencia de Datos
**Propósito:** explicar, sin ambigüedad, la necesidad real detrás de dos propuestas de cambio al esquema. Ninguna de las dos rompe nada existente — ambas son aditivas.

---

## 1. `category_budget_targets` + columnas nuevas en `transactions`

### El problema que resuelve

El brief del hackathon pide explícitamente: *"identificar hábitos financieros positivos o de riesgo"* y *"presentar recomendaciones simples para mejorar la salud financiera"*. Hoy, el sistema puede clasificar una transacción en una categoría, pero **no tiene ninguna noción de si esa distribución de gasto es sana o no**. Saber que alguien gastó $180.000 en Entretenimiento no dice nada por sí solo — hace falta un punto de comparación.

### La solución: un presupuesto de referencia, no inventado

`category_budget_targets` guarda, por categoría, **qué porcentaje del gasto total es razonable** que represente. Los valores para Chile salen de la **IX Encuesta de Presupuestos Familiares del INE (2022-2023)** — un dato público, no un número que nos inventamos. Con esto, el sistema puede comparar el % real de gasto de un usuario contra el % recomendado, y generar una alerta cuando hay desbalance (ej. "estás gastando 22% en entretenimiento, lo recomendado es 5%").

**Por qué es una tabla y no una constante en el código:** porque los porcentajes van a variar por país (ya quedó pensada con columna `country_code`), y porque son datos, no lógica — no debería hacer falta un deploy del backend para ajustar un porcentaje si el equipo decide, por ejemplo, que el de Vivienda debería ser 22% en vez de 20%.

### Las columnas nuevas en `transactions`

No son un capricho — cada una resuelve algo que ya se discutió y decidió en el proyecto, y que sin estas columnas no se podía representar:

| Columna | Qué problema resuelve |
|---|---|
| `source` (BANK/MANUAL) | Sin esto, no hay forma de distinguir una transacción que vino de una cartola de una que el usuario ingresó a mano — y esa distinción es la base de la Alternativa 1 (registro manual) que se definió con el equipo. |
| `payment_method` | El usuario puede pagar en efectivo o con débito — el sistema necesita saberlo para decidir si vale la pena intentar conciliar ese registro contra una cartola (el efectivo nunca va a aparecer en un banco). |
| `link_status` / `linked_transaction_id` | Sin esto, un mismo gasto pagado en efectivo y luego visto en la cartola se contaría dos veces. Estas columnas son las que permiten decir "este registro manual y esta transacción bancaria son la misma cosa". |
| `category_method` | Distingue si una categoría vino de un mapeo, una regla, el modelo, o el usuario. Sin esto, no hay forma de saber qué datos son confiables para reentrenar el clasificador más adelante — todo se vería igual de "cierto". |
| `category_confidence` | Es la probabilidad que exige el propio ejemplo de salida del brief (`"probabilidad": 0.82`). Sin este campo, no hay dónde guardarla. |

### Qué pasa si no se hacen estos cambios

El sistema podría seguir clasificando transacciones, pero **no podría generar ninguna recomendación de presupuesto** (no hay contra qué comparar), **no podría distinguir gasto real de duplicado** (efectivo + cartola contando dos veces), y **no habría trazabilidad para mejorar el clasificador con el tiempo**.

---

## 2. `financial_profile_history` — el "evolutivo del cliente"

### El problema que resuelve

Hoy, `users.financial_profile` (Saludable/En observación/En riesgo, o el equivalente real `SAVER/BALANCED/SPENDER/AT_RISK`) se **sobreescribe** cada vez que se actualiza. Eso significa que si alguien mejora su situación financiera de "En riesgo" a "Saludable" en 3 meses, **no hay forma de mostrar esa evolución** — solo se ve el estado actual, como una foto, nunca una película.

Esto no es un detalle menor: es un requisito explícito del brief (*"realizar un seguimiento de la evolución del comportamiento financiero a lo largo del tiempo"*), y es además la base técnica de algo que el equipo definió como objetivo: el **"evolutivo del cliente"**.

### Qué es el "evolutivo del cliente", en simple

Es el paso intermedio entre un reporte de BI tradicional (una foto del estado actual) y un modelo de Machine Learning completo (que aprende patrones de comportamiento automáticamente). En vez de comparar el gasto de un usuario contra un porcentaje fijo del sistema (como hace `category_budget_targets`), se compara contra **el propio promedio histórico de ese usuario** — usando estadística simple (media y desviación), no un modelo entrenado.

Esto le da personalización real al sistema sin la complejidad de entrenar y mantener un modelo de comportamiento — que es justo la preocupación que se planteó en su momento: *"no quiero algo tan complicado ni complejo en su implementación"*.

### Por qué necesita una tabla aparte, y no alcanza con `users`

Una tabla de historial, por definición, tiene que guardar múltiples filas por usuario a lo largo del tiempo — algo que una columna en `users` no puede hacer sin perder la información anterior en cada actualización. `financial_profile_history` guarda una fila cada vez que se actualiza el perfil (enlazada por FK a `users`), sin tocar la tabla `users` en absoluto.

### Qué pasa si no se hace este cambio

El sistema seguiría funcionando para mostrar el estado actual de un usuario, pero **nunca podría mostrar una gráfica de evolución**, y el "evolutivo del cliente" —la pieza intermedia entre BI y ML que se definió como parte de la estrategia del proyecto— **no tendría datos sobre los cuales construirse**, sin importar cuánto código se escriba para calcularlo.

---

## 3. En una frase, para cada uno

- **`category_budget_targets` + columnas en `transactions`:** sin esto, no hay recomendaciones de presupuesto, ni forma de evitar contar un gasto dos veces, ni trazabilidad para mejorar el clasificador.
- **`financial_profile_history`:** sin esto, no hay evolución del cliente que mostrar — literalmente no hay datos guardados para eso, sin importar qué tan buena sea la lógica que se escriba encima.
