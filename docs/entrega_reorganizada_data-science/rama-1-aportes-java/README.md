# Rama 1 — Aportes en Java

**Cómo usar esta carpeta:** todo lo que está acá adentro respeta la ruta exacta del paquete real (`backend/src/main/java/com/g9latam/team62/fintech_api/...`). Se puede copiar directo encima del repositorio local, reemplazando los archivos modificados y agregando los nuevos.

**Guía completa de commits y descripción de PR:** `../docs/guia-branch-y-pr.md`
**Por qué cada tabla/columna nueva:** `../docs/justificacion-cambios-bd.md`

## Qué trae

- `model/`: `Transaction.java` (modificado), `TransactionSource`, `PaymentMethod`, `LinkStatus`, `CategoryMethod`, `CategoryBudgetTarget`, `FinancialProfileHistory` (nuevos)
- `dto/`: `ManualTransactionRequest`, `CategoryCorrectionRequest` (nuevos)
- `repository/`: `TransactionRepository` (modificado, +1 método), `CategoryBudgetTargetRepository`, `FinancialProfileHistoryRepository` (nuevos)
- `service/`: `TransactionService` (modificado, +2 métodos), `UserService` (modificado, +1 hook), `BudgetRecommendationService` (nuevo)
- `controller/`: `TransactionController` (modificado, +2 endpoints), `RecommendationController` (modificado, +1 endpoint)
- `db/oracle/002_*.sql` y `003_*.sql`: migraciones incrementales, se aplican en ese orden, después de `schema.sql` + `data.sql` reales

## Antes de mergear

```bash
cd backend
./mvnw compile
./mvnw test
```

No se pudo correr en el entorno de trabajo (sin salida a Maven Central) — es el único paso que falta validar.
