# Finance AI — Arquitectura actual (trazada hasta hoy)

**Diferencia con `01-arquitectura-decisiones.md`:** aquel documento describe el diseño de las primeras semanas (VCN de 3 capas, Load Balancer, subredes públicas/privadas). **Ese diseño no es lo que está desplegado hoy.** Este documento describe la arquitectura real, verificada contra el repositorio, más las piezas que ya integramos y las que faltan.

---

## 1. Diagrama

```
Frontend  ──────▶  Backend API  ──────▶  Base de datos
(mock, sin           (Spring Boot,          (H2 en dev /
 conectar aún)         Docker + GHCR)         Oracle ATP en prod)
                            │
                            ▼
              ┌─────────────────────────────────┐
              │   Aportes de Data Science         │
              │   ya integrados en el backend:    │
              │   · Registro manual                │
              │   · Motor de recomendaciones       │
              │   · Historial de perfil financiero │
              └─────────────────────────────────┘
                     ▲                    ▲
                (pendiente)          (pendiente)
                     │                    │
        ┌────────────────────┐  ┌──────────────────────┐
        │ Script ingesta       │  │ Modelo ML             │
        │ cartolas (Python)    │  │ clasificador           │
        └────────────────────┘  └──────────────────────┘
```

**Cómo leer los recuadros punteados:** son las dos piezas que ya están construidas y probadas de nuestro lado, pero que todavía no están conectadas al flujo real — necesitan que Backend construya el punto de integración (ver `contrato-ingesta-cartolas.md` para la primera).

## 2. Las 3 capas reales

| Capa | Qué es hoy | Qué NO es |
|---|---|---|
| **Frontend** | React + Vite, con un flujo de páginas armado (landing, auth, análisis) | No consume la API real — la lógica de análisis está mockeada en el cliente, con su propia taxonomía de categorías |
| **Backend** | Spring Boot 4.1, JPA, Spring Security. Se construye y publica a GHCR vía GitHub Actions; se despliega con `docker-compose` | No está desplegado sobre la VCN de 3 capas diseñada al inicio del proyecto — es un despliegue de contenedores directo, más simple |
| **Base de datos** | Perfil `dev`: H2 en memoria, se genera sola desde las entidades. Perfil `oracle`: Oracle Autonomous Database vía wallet (mTLS), schema aplicado a mano | No usa Flyway/Liquibase — los cambios se aplican con scripts `ALTER TABLE` numerados |

## 3. Lo que ya está integrado (parte de abajo del diagrama)

Tres funcionalidades completas, en Java, ya escritas y revisadas (aunque todavía no compiladas con Maven real ni mergeadas al repositorio):

1. **Registro manual de transacciones** — captura gastos que el banco nunca ve (efectivo), con conciliación futura contra la cartola real.
2. **Motor de Recomendaciones Presupuestarias** — compara el gasto real contra un presupuesto de referencia (datos del INE, no inventados) y genera alertas. Sin Machine Learning, a propósito.
3. **Historial de perfil financiero** — permite mostrar la evolución del perfil de un usuario en el tiempo, algo que antes no existía.

## 4. Lo que falta (los dos recuadros punteados)

1. **Script de ingesta de cartolas** — construido y probado (`procesar_cartola_cli.py`, contra los 4 bancos reales), pero el endpoint que lo invoque y la escritura en base de datos son responsabilidad de Backend. Contrato completo en `contrato-ingesta-cartolas.md`.
2. **Modelo de Machine Learning para clasificar texto libre** — es el único entregable obligatorio del brief del hackathon que todavía no se construyó. El dataset simulado (`generar_dataset.py`) ya está listo para entrenar sobre él.

## 5. Piezas de diseño que quedaron en el camino, sin implementar

- El **plan de clasificación híbrido de 4 niveles** (mapeo aprendido → reglas → modelo → fallback), diseñado en conjunto con un compañero de Backend, sigue sin código en ningún lado — ni la parte de reglas (Backend) ni la del modelo (nosotros). No bloquea nada de lo ya construido.
- El **job de conciliación** entre un registro manual y la transacción real de la cartola (mismo monto, ventana de 2-3 días) tiene el modelo de datos listo, pero el proceso en sí no está escrito.

---

*Para el detalle técnico completo de cada tabla y endpoint, ver `documentacion-tecnica.md`. Para el resumen pensado para leer de una sola vez, ver `RESUMEN-PARA-BACKEND.md`.*
