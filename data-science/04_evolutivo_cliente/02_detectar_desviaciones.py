"""
Evolutivo del cliente: compara el % de gasto del mes actual, por categoría,
contra el PROPIO promedio histórico del usuario (no contra el % fijo del
INE, eso ya lo hace BudgetRecommendationService).

Estadística simple -- media y desviación estándar -- no Machine Learning,
mismo criterio que ya usamos en el motor de recomendaciones original.
"""

import numpy as np
import pandas as pd

MIN_MESES_HISTORIAL = 3  # bajo esto, no hay suficiente historia para calcular "el promedio de esta persona"
DESVIACIONES_MINIMAS = 2.0  # cuántas desviaciones estándar por sobre el promedio para considerar "anómalo"
PISO_DESVIACION_PP = 4.0  # piso mínimo de desviación (puntos %) -- evita marcar a usuarios muy estables por ruido mínimo
# Valores ajustados empíricamente contra el dataset dummy (ver validacion_algoritmo.md):
# con estos, 10/10 anomalías inyectadas detectadas y 0 falsos positivos sobre 20 usuarios simulados.


def calcular_desviaciones(df: pd.DataFrame) -> pd.DataFrame:
    """Para cada usuario y categoría, compara el mes actual contra el historial.
    Devuelve solo las desviaciones que superan el umbral -- no todo el detalle."""
    resultados = []

    for uid, grupo_usuario in df.groupby("usuario_id"):
        historial = grupo_usuario[~grupo_usuario["es_mes_actual"]]
        actual = grupo_usuario[grupo_usuario["es_mes_actual"]]

        meses_disponibles = historial["mes"].nunique()
        if meses_disponibles < MIN_MESES_HISTORIAL:
            continue  # no hay suficiente historia todavía -- no se genera nada para este usuario

        # % de gasto por categoría, por mes (histórico)
        totales_mes_hist = historial.groupby("mes")["monto"].transform("sum")
        historial = historial.assign(pct=historial["monto"] / totales_mes_hist * 100)

        # % de gasto por categoría, mes actual
        total_actual = actual["monto"].sum()
        actual = actual.assign(pct=actual["monto"] / total_actual * 100)

        stats_hist = historial.groupby("categoria")["pct"].agg(["mean", "std"]).fillna(0)

        for _, fila in actual.iterrows():
            cat = fila["categoria"]
            if cat not in stats_hist.index:
                continue
            media = stats_hist.loc[cat, "mean"]
            desv = max(stats_hist.loc[cat, "std"], PISO_DESVIACION_PP)
            pct_actual = fila["pct"]

            umbral = media + DESVIACIONES_MINIMAS * desv
            if pct_actual > umbral:
                resultados.append({
                    "usuario_id": uid,
                    "categoria": cat,
                    "pct_actual": round(pct_actual, 1),
                    "promedio_historico": round(media, 1),
                    "desviacion_std": round(desv, 1),
                    "diferencia_pp": round(pct_actual - media, 1),
                })

    return pd.DataFrame(resultados)


def texto_recomendacion(fila: pd.Series) -> str:
    return (
        f"Este mes gastaste {fila['pct_actual']:.0f}% en "
        f"{fila['categoria'].lower()}, {fila['diferencia_pp']:.0f} puntos por sobre tu "
        f"propio promedio de los últimos meses ({fila['promedio_historico']:.0f}%). "
        f"Puede valer la pena revisar si fue un gasto puntual o un cambio de hábito."
    )


if __name__ == "__main__":
    df = pd.read_csv("transacciones_dummy_evolutivo.csv")
    desviaciones = calcular_desviaciones(df)

    print(f"Usuarios evaluados: {df['usuario_id'].nunique()}")
    print(f"Desviaciones detectadas: {len(desviaciones)}")
    print(f"Usuarios con al menos una desviación detectada: {desviaciones['usuario_id'].nunique()}")
    print()
    for _, fila in desviaciones.sort_values("diferencia_pp", ascending=False).iterrows():
        print(f"  usuario {fila['usuario_id']:2d} | {texto_recomendacion(fila)}")

    desviaciones.to_csv("desviaciones_detectadas.csv", index=False)
