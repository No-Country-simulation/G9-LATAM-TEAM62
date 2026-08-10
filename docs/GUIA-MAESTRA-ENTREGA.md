# Guía de la entrega — qué mostrar, y a quién

**Propósito de este documento:** es el punto de partida para cualquier conversación con Backend. No reemplaza a los demás documentos — te dice cuál de ellos abrir según la pregunta.

---

## Hay DOS entregas independientes

| | Entrega 1 — Código Java | Entrega 2 — Contrato de ingesta |
|---|---|---|
| **Qué es** | 3 funcionalidades ya escritas en Java (registro manual, motor de recomendaciones, historial de perfil) | Script Python probado + documento de contrato — Backend construye el endpoint |
| **Toca código de Backend** | Sí — `Transaction.java`, `TransactionService.java`, `UserService.java`, etc. (todo aditivo, nada se rompe) | No — cero archivos `.java` |
| **Rama sugerida** | `feature/data-science-aportes` | `feature/data-science-ingesta-cartolas` |
| **Documento para la conversación** | `guia-branch-y-pr.md` | `contrato-ingesta-cartolas.md` |
| **Si preguntan "por qué"** | `justificacion-cambios-bd.md` | La sección 7 del propio contrato ("Qué NO es responsabilidad nuestra") |
| **Estado** | Construido y probado manualmente (sin Maven real disponible en el entorno de trabajo — falta `./mvnw compile && ./mvnw test`) | Script probado contra los 4 bancos reales; el endpoint todavía no existe, es trabajo de Backend |

---

## Cheat sheet — pregunta → documento

| Si preguntan... | Abrí... |
|---|---|
| "¿Qué es todo esto?" | `documentacion-funcional.md` (sin jerga) o `actividades-data-science.md` (bitácora completa) |
| "¿Cómo está armada la base de datos?" | `documentacion-tecnica.md`, sección 3 |
| "¿Por qué necesitan esta tabla/columna?" | `justificacion-cambios-bd.md` |
| "¿Cómo integro el script de cartolas?" | `contrato-ingesta-cartolas.md` |
| "¿Cómo pruebo esto en mi máquina?" | `probar-localmente.md` |
| "¿Cómo subo esto a una rama?" | `guia-branch-y-pr.md` (Java) o `guia-branch-data-science.md` (Python) — según cuál |
| "¿Qué pasó con la propuesta de clasificación del compañero?" | `plan-clasificacion-unificado.md` (tiene una nota de estado al inicio: sigue sin implementarse, no bloquea nada) |

---

## Los 3 puntos que más probablemente pregunten, con la respuesta corta

**1. "¿Por qué son dos ramas y no una?"**
Para que se puedan revisar y mergear por separado — una toca código Java existente, la otra no toca nada de Backend. Mezclarlas haría más difícil el review.

**2. "¿Esto ya está probado?"**
El script Python sí, de punta a punta, contra los 4 bancos reales. El código Java se revisó a mano con mucho cuidado (referencias cruzadas, imports, coherencia con las entidades reales), pero **no se compiló con Maven real** — no había salida de red a Maven Central en el entorno de trabajo. Es lo único pendiente de validar antes de mergear con total confianza.

**3. "¿Esto rompe algo de lo que ya tienen?"**
No — todo lo entregado es aditivo. Ningún endpoint, tabla, ni comportamiento existente cambia; solo se agregan columnas, tablas y endpoints nuevos.

---

## Todos los documentos, para referencia completa

```
docs/
├── 00-briefing-proyecto.md                 Brief original del hackathon
├── 01-arquitectura-decisiones.md           Arquitectura, stack, modelo E-R
├── 02-resumen-proyecto.md                  Resumen ejecutivo
├── documentacion-tecnica.md                 Arquitectura + modelo de datos real, completo
├── documentacion-funcional.md               Lo mismo, sin jerga técnica
├── actividades-data-science.md              Bitácora completa de todo lo hecho
├── plan-clasificacion-equipo-datos.md       Nuestra propuesta de clasificación (ML)
├── plan-clasificacion-unificado.md          Los 4 niveles unificados con la propuesta de Backend
├── plan-clasificacion-comparacion.md        Diferencias/similitudes entre ambas propuestas
├── plan-accion-motor-recomendaciones.md     Plan del motor de recomendaciones (porcentajes INE)
├── justificacion-cambios-bd.md              Por qué cada tabla/columna nueva (Entrega 1)
├── contrato-ingesta-cartolas.md             Contrato para Backend (Entrega 2)  ← el que pediste
├── guia-branch-y-pr.md                      Rama + PR para el código Java (Entrega 1)
├── guia-branch-data-science.md              Rama + PR para el script Python (Entrega 2)
├── probar-localmente.md                     Cómo correr el backend en tu máquina
└── handoff-backend.md                       (documento de traspaso de una conversación anterior — desactualizado, no usar)
```

**Un archivo a ignorar:** `handoff-backend.md` quedó de una etapa anterior del proyecto y ya no refleja el estado actual — todo lo que decía está superado por `documentacion-tecnica.md` y `actividades-data-science.md`. No lo lleves a la reunión.
