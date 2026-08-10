"""
Motor de Recomendaciones Presupuestarias — Finance AI

Misma lógica que BudgetRecommendationService.java, en Python, pensada para:
  - correr dentro del notebook de análisis (sobre el DataFrame que ya produce
    analisis_cartola.ipynb: columnas FECHA/DESCRIPCION/CATEGORIA/TIPO_MOVIMIENTO/MONTO)
  - o servirse como un microservicio Docker aparte si el equipo lo prefiere así

No usa Machine Learning a propósito: es comparación de porcentajes contra una
tabla de referencia, con niveles de severidad, umbral mínimo de datos, chequeo
de tasa de ahorro, tope de recomendaciones por corrida, y enfriamiento.

IMPORTANTE: nos adaptamos a las 13 categorías reales de Category.java del
backend -- no se agrega ninguna categoría nueva. Las transferencias (TEF,
giros) se guardan igual, con la categoría que les asigne el clasificador
(normalmente OTHER_EXPENSE/OTHER_INCOME según dirección), y se excluyen acá
del cálculo de % de gasto detectándolas por texto en la descripción -- el
dinero se sigue registrando, solo no cuenta como gasto/ingreso de consumo.
"""

from __future__ import annotations

import re
from dataclasses import dataclass
from datetime import datetime, timedelta
from enum import Enum


# ---------------------------------------------------------------------------
# Categorías y porcentajes de referencia (mismos datos que category_budget_targets.sql)
# Alineadas 1:1 con Category.java / TransactionType.java reales del backend.
# ---------------------------------------------------------------------------

class TransactionType(Enum):
    INCOME = "INCOME"
    EXPENSE = "EXPENSE"
    SAVING = "SAVING"


CATEGORY_TYPE = {
    "FOOD": TransactionType.EXPENSE,
    "TRANSPORT": TransactionType.EXPENSE,
    "HOUSING": TransactionType.EXPENSE,
    "UTILITIES": TransactionType.EXPENSE,
    "ENTERTAINMENT": TransactionType.EXPENSE,
    "HEALTH": TransactionType.EXPENSE,
    "EDUCATION": TransactionType.EXPENSE,
    "SHOPPING": TransactionType.EXPENSE,
    "OTHER_EXPENSE": TransactionType.EXPENSE,
    "SALARY": TransactionType.INCOME,
    "OTHER_INCOME": TransactionType.INCOME,
    "INVESTMENT": TransactionType.SAVING,
    "SAVINGS": TransactionType.SAVING,
}

# Mismas palabras clave que TRANSFER_KEYWORDS en el notebook -- una
# transferencia sigue apareciendo en los datos (normalmente categorizada
# como OTHER_EXPENSE/OTHER_INCOME), pero se excluye acá del % de gasto.
TRANSFER_PATTERN = re.compile(r"\b(TRANSF|TEF|GIRO|TRANSFERENCIA)\b", re.IGNORECASE)


def is_transfer(description: str) -> bool:
    return bool(description) and TRANSFER_PATTERN.search(description) is not None


# Fuente: INE Chile, IX Encuesta de Presupuestos Familiares (EPF) 2022-2023.
# Ver infra/oci/category_budget_targets.sql para las notas de cómo se mapeó
# cada categoría de la EPF a las categorías del sistema.
BUDGET_TARGETS_CL = {
    "FOOD": 28.00,
    "HOUSING": 20.00,
    "TRANSPORT": 14.00,
    "UTILITIES": 9.00,
    "HEALTH": 7.00,
    "EDUCATION": 6.00,
    "OTHER_EXPENSE": 6.00,
    "SHOPPING": 5.00,
    "ENTERTAINMENT": 5.00,
}

# Mapeo de las categorías que produce analisis_cartola.ipynb hacia las
# categorías reales del backend (Category.java). "Otros" -> OTHER_EXPENSE,
# "SERVICES" -> UTILITIES, "TRANSACTIONS" queda igual (ya es TRANSFER).

# ---------------------------------------------------------------------------
# NOTA: ya NO hace falta traducir categorías del notebook -> backend.
# analisis_cartola.ipynb (versión actualizada) emite directamente las 13
# categorías reales de Category.java, así que la columna CATEGORIA del
# DataFrame ya viene lista para usar tal cual acá abajo.
# ---------------------------------------------------------------------------


# ---------------------------------------------------------------------------
# Parámetros del motor
# ---------------------------------------------------------------------------

MIN_TRANSACTIONS = 5
MODERATE_RATIO = 1.2
HIGH_RATIO = 1.5
SEVERE_RATIO = 2.0
MAX_RECOMMENDATIONS_PER_RUN = 3
COOLDOWN_DAYS = 7
TARGET_SAVINGS_RATE = 0.20

CATEGORY_LABELS_ES = {
    "FOOD": "alimentación",
    "TRANSPORT": "transporte",
    "HOUSING": "vivienda",
    "UTILITIES": "servicios básicos",
    "ENTERTAINMENT": "entretenimiento",
    "HEALTH": "salud",
    "EDUCATION": "educación",
    "SHOPPING": "compras",
    "OTHER_EXPENSE": "otros gastos",
}


@dataclass
class Transaction:
    category: str          # una de las 13 categorías reales de Category.java
    amount: float           # siempre positivo
    date: datetime
    description: str = ""   # necesaria para detectar transferencias vía is_transfer()


@dataclass
class CategoryDeviation:
    category: str
    actual_percentage: float
    recommended_percentage: float
    ratio: float


def _sum_by_type(transactions: list[Transaction], t_type: TransactionType) -> dict[str, float]:
    totals: dict[str, float] = {}
    for t in transactions:
        if CATEGORY_TYPE.get(t.category) != t_type:
            continue
        if is_transfer(t.description):
            continue  # egreso/ingreso real, pero no es gasto/ingreso de consumo
        totals[t.category] = totals.get(t.category, 0.0) + t.amount
    return totals


def _severity_label(ratio: float) -> str:
    if ratio >= SEVERE_RATIO:
        return "muy por sobre lo recomendado"
    if ratio >= HIGH_RATIO:
        return "bastante por sobre lo recomendado"
    return "levemente por sobre lo recomendado"


def compute_deviations(transactions: list[Transaction], targets: dict[str, float] = BUDGET_TARGETS_CL
                        ) -> tuple[list[CategoryDeviation], float]:
    """Devuelve las desviaciones (>= MODERATE_RATIO) y el gasto total del período.
    Las transferencias (TRANSACTIONS) quedan excluidas del total de gasto."""
    expense_by_category = _sum_by_type(transactions, TransactionType.EXPENSE)
    total_expense = sum(expense_by_category.values())

    deviations: list[CategoryDeviation] = []
    if total_expense <= 0:
        return deviations, total_expense

    for category, recommended in targets.items():
        category_total = expense_by_category.get(category, 0.0)
        actual_percentage = category_total / total_expense * 100
        ratio = actual_percentage / recommended
        if ratio >= MODERATE_RATIO:
            deviations.append(CategoryDeviation(category, actual_percentage, recommended, ratio))

    return deviations, total_expense


def build_category_text(d: CategoryDeviation) -> str:
    label = CATEGORY_LABELS_ES.get(d.category, d.category)
    severity = _severity_label(d.ratio)
    return (f"Gasto {severity} en {label}: {d.actual_percentage:.1f}% de tu gasto total "
            f"(referencia: {d.recommended_percentage:.1f}%). "
            f"Considera revisar los gastos recurrentes en esta categoría.")


def build_savings_text(transactions: list[Transaction], total_expense: float) -> str | None:
    total_income = sum(_sum_by_type(transactions, TransactionType.INCOME).values())
    if total_income <= 0:
        return None
    savings_rate = (total_income - total_expense) / total_income
    if savings_rate >= TARGET_SAVINGS_RATE:
        return None
    return (f"Tu tasa de ahorro este período fue de {savings_rate * 100:.1f}%, "
            f"bajo el objetivo de {TARGET_SAVINGS_RATE * 100:.0f}%. "
            f"Aumentar la frecuencia de ahorro puede ayudarte a construir un margen frente a imprevistos.")


def generate_recommendations(transactions: list[Transaction],
                              last_recommendation_at: datetime | None = None,
                              targets: dict[str, float] = BUDGET_TARGETS_CL) -> list[str]:
    """Punto de entrada principal. Replica BudgetRecommendationService.generateRecommendations().

    - `transactions`: ya en el período a analizar (ej. últimos 30 días) -- este
      módulo no filtra por fecha, eso se resuelve antes de llamarlo.
    - `last_recommendation_at`: si se pasa y está dentro del cooldown, no genera nada.
    """
    if last_recommendation_at is not None:
        if datetime.now() - last_recommendation_at < timedelta(days=COOLDOWN_DAYS):
            return []

    if len(transactions) < MIN_TRANSACTIONS:
        return []

    deviations, total_expense = compute_deviations(transactions, targets)
    if total_expense <= 0:
        return []

    deviations.sort(key=lambda d: d.ratio, reverse=True)
    texts = [build_category_text(d) for d in deviations[:MAX_RECOMMENDATIONS_PER_RUN]]

    savings_text = build_savings_text(transactions, total_expense)
    if savings_text:
        texts.append(savings_text)

    return texts


# ---------------------------------------------------------------------------
# Ejemplo de uso a partir del DataFrame de analisis_cartola.ipynb
# ---------------------------------------------------------------------------
if __name__ == "__main__":
    # Simulación rápida sin pandas, solo para mostrar la forma de uso.
    # Notar la transferencia: queda como OTHER_EXPENSE (misma categoría que le
    # asignaría el notebook a un "PAGO A TERCERO" tipo TEF), pero is_transfer()
    # la detecta por la descripción y se excluye igual del % de gasto.
    ejemplo = [
        Transaction("FOOD", 120_000, datetime(2026, 7, 1), "COMPRA JUMBO PROVIDENCIA"),
        Transaction("ENTERTAINMENT", 180_000, datetime(2026, 7, 3), "NETFLIX.COM"),  # muy por sobre el 5% recomendado
        Transaction("TRANSPORT", 90_000, datetime(2026, 7, 5), "COPEC ESTACION CENTRAL"),
        Transaction("HOUSING", 350_000, datetime(2026, 7, 1), "PAGO GASTO COMUN"),
        Transaction("SALARY", 900_000, datetime(2026, 7, 1), "REMUNERACION CORP CENTRO"),
        Transaction("OTHER_EXPENSE", 200_000, datetime(2026, 7, 2), "TEF PARA JEREMY"),  # transferencia, no cuenta como gasto
    ]
    for texto in generate_recommendations(ejemplo):
        print("-", texto)

# Para usar con el DataFrame real del notebook (columnas FECHA/DESCRIPCION/
# CATEGORIA/TIPO_MOVIMIENTO/MONTO) -- la CATEGORIA ya viene alineada con el
# backend, no hace falta traducir nada:
#
#   transacciones = [
#       Transaction(
#           category=row.CATEGORIA,
#           amount=abs(row.MONTO),
#           date=row.FECHA,
#           description=row.DESCRIPCION,
#       )
#       for row in df.itertuples()
#   ]
#   recomendaciones = generate_recommendations(transacciones)
