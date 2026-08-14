# Finance AI — Documentación Funcional

**Qué es este documento:** una explicación de qué hace Finance AI y cómo lo hace, sin jerga técnica — pensada para quien evalúe el proyecto, para nuevos integrantes del equipo, o para cualquiera que quiera entender el producto sin leer código.

**Su contraparte técnica:** `documentacion-tecnica.md`.

---

## 1. ¿Qué problema resuelve Finance AI?

Mucha gente tiene acceso a los datos de sus movimientos bancarios, pero no logra convertir esa información en decisiones concretas sobre su plata. Finance AI toma esos movimientos y responde tres preguntas simples:

1. **¿En qué se me va la plata?** (clasificación automática de gastos por categoría)
2. **¿Cómo está mi salud financiera?** (un perfil: saludable, en observación, o en riesgo)
3. **¿Qué debería ajustar?** (recomendaciones concretas, no genéricas)

---

## 2. ¿Cómo llegan los datos al sistema?

Hay dos caminos, y **se complementan, no compiten entre sí**:

### 2.1 Cartola bancaria
La persona sube su cartola (Banco Chile, CuentaRUT, Falabella, Mercado Pago — probado con las 4). El sistema la lee, la limpia, y clasifica cada movimiento en una categoría automáticamente.

### 2.2 Registro manual
Cuando alguien paga en efectivo, el banco nunca se entera — así que el usuario puede anotarlo a mano en el momento de la compra (monto, categoría, si fue con débito o efectivo). Esto tiene dos beneficios:
- Completa el panorama financiero real de la persona, no solo lo que pasa por el banco.
- Si más adelante sube la cartola y ese mismo movimiento aparece ahí (mismo monto, fecha cercana), el sistema los **vincula** — no los duplica.

---

## 3. ¿Cómo se clasifican los gastos?

El plan (todavía en construcción, algunas partes ya diseñadas y otras pendientes de programar) funciona en capas, de lo más simple y rápido a lo más sofisticado:

1. **¿Ya vimos esta misma descripción antes?** Si algún usuario ya clasificó "COMPRA JUMBO" como Alimentación, se reutiliza esa respuesta al instante.
2. **¿Contiene una palabra conocida?** Un diccionario de palabras clave (JUMBO, COPEC, NETFLIX, etc.) resuelve los casos obvios.
3. **¿Nada de lo anterior funcionó?** Ahí entra un modelo entrenado con machine learning, capaz de reconocer descripciones parecidas a otras que ya vio, aunque no sean idénticas.
4. **Si ni así se puede clasificar**, queda en una categoría genérica ("Otros"), a la espera de que el usuario la corrija.

Cuando el usuario corrige una categoría, esa corrección **mejora el sistema para todos los usuarios futuros** — no solo para él.

---

## 4. ¿Qué es el perfil financiero?

Una clasificación general de qué tan sana está la situación financiera de la persona: **Saludable**, **En observación**, o **En riesgo**. Se calcula combinando cuánto gasta versus cuánto gana, cuánta deuda tiene, y con qué frecuencia ahorra.

---

## 5. ¿Cómo se generan las recomendaciones?

Esta es la parte que ya está construida y funcionando. La lógica es simple de explicar:

**Existe un "presupuesto ideal" por categoría** — por ejemplo, en Chile, lo típico es que la vivienda no supere el 20% del gasto total, la alimentación el 28%, el entretenimiento el 5%, etc. (Estos números salen de una encuesta real del gobierno de Chile sobre cómo gastan los hogares, no son inventados.)

**El sistema compara tu gasto real contra ese ideal.** Si alguien gasta 22% en entretenimiento cuando lo recomendado es 5%, el sistema lo detecta y genera una alerta — con distintos niveles de urgencia según qué tan grande sea la diferencia (leve, alta, o severa).

**También revisa cuánto estás ahorrando.** Si tu tasa de ahorro (lo que te sobra del sueldo) está bajo el 20% recomendado, te avisa.

**No abruma con alertas.** Como máximo, muestra las 3 categorías más desbalanceadas por vez, y no repite la misma alerta antes de una semana.

**Un detalle importante:** las transferencias entre cuentas (por ejemplo, pasarle plata a un amigo) **no cuentan como gasto** en este análisis — sí quedan registradas como movimiento de dinero, pero no distorsionan el cálculo de en qué categorías gasta realmente la persona.

---

## 6. ¿Por qué no es todo "inteligencia artificial"?

Es una pregunta válida, porque el nombre del proyecto es "Finance AI". La respuesta corta: **se usa IA donde realmente aporta valor, y aritmética simple donde alcanza y sobra.**

- Clasificar texto libre (una descripción bancaria ambigua) **sí necesita** un modelo entrenado, porque no hay una regla fija que cubra todos los casos posibles.
- Comparar un gasto contra un presupuesto de referencia **no necesita** IA — es una resta y una división. Usar un modelo ahí sería más lento, más difícil de explicar, y no aportaría nada mejor.

Esta decisión también fue deliberada por tiempo: con 6 semanas de hackathon, conviene invertir el esfuerzo de Machine Learning donde es indispensable (la clasificación), y resolver el resto con métodos simples y confiables.

---

## 7. ¿Qué tan lejos llega el sistema, hoy?

Para ser honestos sobre el estado real del proyecto:

| Funcionalidad | Estado |
|---|---|
| Lectura de cartolas de múltiples bancos | ✅ Funcionando (probado con datos reales) |
| Clasificación por palabras clave | ✅ Diseñado, con categorías alineadas al sistema real |
| Registro manual de gastos | ✅ Diseñado, listo para integrar |
| Motor de recomendaciones presupuestarias | ✅ Construido y probado, en dos versiones (Java y Python) |
| Modelo de Machine Learning entrenado | ⏳ Dataset de entrenamiento listo; el entrenamiento en sí está pendiente |
| Historial del perfil financiero en el tiempo | ⏳ Pendiente — hoy solo se ve el estado más reciente |
| Aplicación visual (frontend) | ⏳ Solo existe una pantalla de ejemplo, sin conexión al sistema todavía |

---

## 8. Glosario rápido

- **Cartola:** el resumen de movimientos que entrega un banco (lo que en otros países se llama "estado de cuenta").
- **Categoría:** el tipo de gasto o ingreso (Alimentación, Transporte, Sueldo, etc.).
- **Perfil financiero:** el diagnóstico general de salud financiera de una persona.
- **Conciliar:** cuando un registro manual y un movimiento de la cartola resultan ser el mismo gasto, y el sistema los une en vez de contarlos dos veces.
- **Tasa de ahorro:** el porcentaje del ingreso que a una persona le queda después de gastar.
