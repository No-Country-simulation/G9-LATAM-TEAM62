# Obsoleto — NO USAR

Estos 3 archivos (`schema.sql`, `category_budget_targets.sql`, `populate_dummy_data.sql`) son del **diseño exploratorio de las primeras semanas del proyecto**, antes de auditar el repositorio real de Backend. Proponían tablas (`cartola_cargada`, `registro_manual`, `analisis_historial`, `categoria_mapeo`) que **nunca se llegaron a implementar** y que **no coinciden con el schema real** (`backend/src/main/resources/db/oracle/schema.sql`).

Quedan acá únicamente como registro histórico de cómo evolucionó el diseño. **No se suben a ninguna rama, no se ejecutan contra ninguna base de datos real.**

El `category_budget_targets.sql` real y vigente (con `country_code`, alineado a las 9 categorías reales) está en `rama-1-aportes-java/backend/src/main/resources/db/oracle/002_manual_entries_and_budget.sql` — no en este archivo suelto.
