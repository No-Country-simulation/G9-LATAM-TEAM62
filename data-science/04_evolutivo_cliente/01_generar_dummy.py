"""
Generador de datos dummy para probar el "evolutivo del cliente" -- comparar
el gasto actual de un usuario contra su propio promedio histórico.

Diseño del ruido, a propósito:
  - Cada usuario tiene un patrón base de gasto por categoría (su "normalidad").
  - Cada mes, ese patrón varía con ruido gaussiano -- nadie gasta exactamente
    igual mes a mes, ni en la vida real ni acá.
  - A un subconjunto de usuarios se les inyecta una anomalía deliberada en el
    último mes (un cambio de comportamiento real, no ruido) -- para probar
    que el algoritmo SÍ la detecta.
  - Al resto se los deja solo con ruido normal -- para probar que el
    algoritmo NO los marca en falso.

Esto da un "camino feliz" honesto: no es un caso de juguete donde todo
calza perfecto, hay variabilidad real de por medio.
"""

import random
import numpy as np
import pandas as pd
from datetime import date

random.seed(7)
np.random.seed(7)

CATEGORIAS_GASTO = ["FOOD", "TRANSPORT", "HOUSING", "UTILITIES",
                     "ENTERTAINMENT", "HEALTH", "EDUCATION", "SHOPPING"]

N_USUARIOS = 20
N_MESES = 6  # 5 meses de historial + el mes "actual" a evaluar
RUIDO_STD = 3.5  # desviación estándar del ruido mes a mes, en puntos porcentuales
MESES = pd.date_range(end=date(2026, 8, 1), periods=N_MESES, freq="MS")


def patron_base_usuario() -> dict:
    """Genera el patrón de gasto 'normal' de un usuario: pesos que suman 100%."""
    pesos = np.random.dirichlet(np.ones(len(CATEGORIAS_GASTO)) * 2)
    return dict(zip(CATEGORIAS_GASTO, pesos * 100))


def aplicar_ruido(patron: dict) -> dict:
    """Un mes normal: el patrón base + ruido gaussiano, sin cambiar de comportamiento."""
    ruidoso = {cat: max(0.5, pct + np.random.normal(0, RUIDO_STD)) for cat, pct in patron.items()}
    total = sum(ruidoso.values())
    return {cat: pct / total * 100 for cat, pct in ruidoso.items()}


def inyectar_anomalia(patron: dict, categoria: str, incremento_pp: float) -> dict:
    """Simula un cambio de comportamiento real: sube 'categoria' en incremento_pp
    puntos porcentuales, quitándole proporcionalmente al resto."""
    con_anomalia = dict(patron)
    con_anomalia[categoria] = con_anomalia[categoria] + incremento_pp
    total = sum(con_anomalia.values())
    return {cat: pct / total * 100 for cat, pct in con_anomalia.items()}


def generar_dataset():
    filas = []
    usuarios_con_anomalia = {}  # usuario_id -> (categoria, incremento) para validar después

    for uid in range(1, N_USUARIOS + 1):
        patron = patron_base_usuario()
        gasto_total_mensual = np.random.uniform(300_000, 900_000)  # CLP

        tiene_anomalia = uid <= N_USUARIOS // 2  # la mitad SÍ, la mitad NO -- para medir falsos positivos también
        categoria_anomalia = random.choice(CATEGORIAS_GASTO) if tiene_anomalia else None
        incremento = np.random.uniform(12, 25) if tiene_anomalia else 0  # puntos porcentuales

        if tiene_anomalia:
            usuarios_con_anomalia[uid] = (categoria_anomalia, incremento)

        for i, mes in enumerate(MESES):
            es_mes_actual = (i == N_MESES - 1)
            if es_mes_actual and tiene_anomalia:
                distribucion = inyectar_anomalia(patron, categoria_anomalia, incremento)
            else:
                distribucion = aplicar_ruido(patron)

            monto_mes = gasto_total_mensual * np.random.uniform(0.85, 1.15)
            for cat, pct in distribucion.items():
                filas.append({
                    "usuario_id": uid,
                    "mes": mes.strftime("%Y-%m"),
                    "es_mes_actual": es_mes_actual,
                    "categoria": cat,
                    "monto": round(monto_mes * pct / 100, 2),
                })

    df = pd.DataFrame(filas)
    return df, usuarios_con_anomalia


if __name__ == "__main__":
    df, anomalias = generar_dataset()
    df.to_csv("transacciones_dummy_evolutivo.csv", index=False)
    print(f"Generadas {len(df)} filas, {N_USUARIOS} usuarios, {N_MESES} meses.")
    print(f"Usuarios con anomalía inyectada en el último mes: {len(anomalias)}")
    for uid, (cat, inc) in list(anomalias.items())[:5]:
        print(f"  usuario {uid}: +{inc:.1f}pp en {cat}")
