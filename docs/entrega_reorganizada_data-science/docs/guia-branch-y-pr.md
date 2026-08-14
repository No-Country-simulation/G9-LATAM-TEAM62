# Cómo subir este aporte al repositorio — guía de rama y PR

## Sí, tiene sentido pedirle ayuda a GitHub Copilot para esto

Copilot (en VS Code, o el agente de Copilot en GitHub.com) puede ayudarte a crear la rama, armar los commits, y hasta redactar la descripción del PR — pero la creación de la rama y el push necesitan tus credenciales de git, así que ese paso lo tienes que hacer vos (o pedírselo a Copilot Chat mientras trabajás localmente). Acá te dejo todo listo para que se lo pases directo.

## 1. Crear la rama

```bash
git checkout main
git pull origin main
git checkout -b feature/data-science-aportes
```

Nombre sugerido: `feature/data-science-aportes` — describe el contenido, no quién lo hizo, que es la convención más común.

## 2. Qué archivos copiar (del zip `G9-LATAM-TEAM62-main-reconciliado.zip`)

Todo lo que está en `backend/` de ese zip ya viene mergeado sobre la estructura real — se puede copiar directo encima del repo local, reemplazando los archivos modificados y agregando los nuevos. Lista completa en `docs/documentacion-tecnica.md`, sección 6 ("Inventario de archivos de la entrega").

## 3. Commits sugeridos (separados por tema, más fácil de revisar)

```bash
git add backend/.../model/{TransactionSource,PaymentMethod,LinkStatus,CategoryMethod}.java \
        backend/.../model/Transaction.java \
        backend/.../dto/ManualTransactionRequest.java \
        backend/.../dto/CategoryCorrectionRequest.java \
        backend/.../service/TransactionService.java \
        backend/.../controller/TransactionController.java
git commit -m "feat(transactions): registro manual y corrección de categoría"

git add backend/.../model/CategoryBudgetTarget.java \
        backend/.../repository/CategoryBudgetTargetRepository.java \
        backend/.../repository/TransactionRepository.java \
        backend/.../service/BudgetRecommendationService.java \
        backend/.../controller/RecommendationController.java \
        backend/src/main/resources/db/oracle/002_manual_entries_and_budget*.sql
git commit -m "feat(recommendations): motor de recomendaciones presupuestarias (INE)"

git add backend/.../model/FinancialProfileHistory.java \
        backend/.../repository/FinancialProfileHistoryRepository.java \
        backend/.../service/UserService.java \
        backend/src/main/resources/db/oracle/003_financial_profile_history*.sql
git commit -m "feat(users): historial de perfil financiero (evolutivo del cliente)"

git add data-science/ docs/
git commit -m "docs(data-science): notebook actualizado, plan de clasificación, documentación técnica y funcional"
```

## 4. Antes de abrir el PR

```bash
cd backend
./mvnw compile
./mvnw test
```

Esto no se pudo verificar del lado nuestro (sin salida a Maven Central en el entorno de trabajo) — es el único paso de validación que falta antes de mergear con confianza.

## 5. Descripción sugerida del PR

```markdown
## Qué trae este PR

Tres funcionalidades de Data Science/Arquitectura, más documentación:

1. **Registro manual de transacciones** — `POST /api/transactions/manual` y
   `PUT /api/transactions/{id}/category`, con trazabilidad de origen
   (banco/manual) y de cómo se determinó cada categoría.
2. **Motor de Recomendaciones Presupuestarias** — compara el gasto real por
   categoría contra un presupuesto de referencia (datos del INE, no
   inventados) y genera alertas. Sin Machine Learning, a propósito — ver
   `docs/plan-accion-motor-recomendaciones.md`.
3. **Historial de perfil financiero** — nueva tabla enlazada a `users`, para
   poder mostrar la evolución del perfil financiero en el tiempo (hoy se
   sobreescribe).

## Qué NO toca

- `Category.java` / `TransactionType.java` sin cambios — todo lo nuevo se
  adapta al enum real, no le pide cambios.
- Ningún endpoint ni comportamiento existente cambia — todo es aditivo.
- Frontend no se toca en este PR.

## Cómo aplicar la migración

```
schema.sql → data.sql → 002_manual_entries_and_budget.sql → 003_financial_profile_history.sql
```

## Documentación

- `docs/documentacion-tecnica.md` — arquitectura, modelo de datos, estado real
- `docs/documentacion-funcional.md` — misma info, sin jerga técnica
- `docs/actividades-data-science.md` — bitácora completa de esta área
- `docs/justificacion-cambios-bd.md` — por qué cada tabla/columna nueva

## Pendiente antes de mergear

- [ ] `./mvnw compile && ./mvnw test` (no se pudo correr en el entorno de trabajo)
```
