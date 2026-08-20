"""
Generador de dataset simulado - Finance AI (Hackathon ONE Alura Latam + Oracle)

Genera:
  - usuarios.csv       -> 1 fila por usuario simulado
  - transacciones.csv  -> N transacciones por usuario, a lo largo de 3 meses

Reproducible: semilla fija (RANDOM_SEED).
"""

import numpy as np
import pandas as pd
from faker import Faker
from datetime import datetime, timedelta
import uuid

RANDOM_SEED = 42
N_USUARIOS = 2200
MESES_HISTORIAL = 3
FECHA_FIN = datetime(2026, 7, 13)

np.random.seed(RANDOM_SEED)
fake = Faker("es_ES")
Faker.seed(RANDOM_SEED)

# ---------------------------------------------------------------------------
# 1. Categorías y comercios de ejemplo (para dar variedad textual realista)
# ---------------------------------------------------------------------------
CATEGORIAS = {
    "Alimentación": {
        "comercios": ["Supermercado Lider", "Jumbo", "Feria libre", "Restaurante El Fogón",
                      "Panadería San José", "Almacén de barrio", "Delivery Rappi Comida"],
        "rango_valor": (8, 180),
        "peso_base": 0.28,
    },
    "Transporte": {
        "comercios": ["Copec Combustible", "Uber", "Metro tarjeta BIP", "Estacionamiento Centro",
                      "Taller mecánico", "Peaje autopista"],
        "rango_valor": (5, 120),
        "peso_base": 0.16,
    },
    "Salud": {
        "comercios": ["Farmacia Cruz Verde", "Clínica Santa María", "Consulta dental",
                      "Seguro complementario", "Óptica Rotter"],
        "rango_valor": (10, 250),
        "peso_base": 0.10,
    },
    "Vivienda": {
        "comercios": ["Arriendo departamento", "Gastos comunes", "Ferretería Sodimac",
                      "Reparación hogar"],
        "rango_valor": (50, 600),
        "peso_base": 0.20,
    },
    "Educación": {
        "comercios": ["Matrícula colegio", "Curso online Alura", "Librería Nacional",
                      "Universidad - mensualidad"],
        "rango_valor": (15, 400),
        "peso_base": 0.08,
    },
    "Ocio": {
        "comercios": ["Streaming Netflix", "Streaming Spotify", "Cine Hoyts", "Bar/Restaurante nocturno",
                      "Viaje fin de semana", "Videojuegos Steam"],
        "rango_valor": (5, 200),
        "peso_base": 0.12,
    },
    "Servicios": {
        "comercios": ["Cuenta de luz", "Cuenta de agua", "Internet + telefonía", "Plan celular",
                      "Seguro de auto"],
        "rango_valor": (10, 150),
        "peso_base": 0.06,
    },
}
CATS = list(CATEGORIAS.keys())
PESOS_BASE = np.array([CATEGORIAS[c]["peso_base"] for c in CATS])
PESOS_BASE = PESOS_BASE / PESOS_BASE.sum()

FREC_AHORRO = ["Baja", "Media", "Alta"]
FREC_AHORRO_SCORE = {"Baja": 0.0, "Media": 0.5, "Alta": 1.0}


def generar_ingreso():
    # Lognormal sesgado hacia ingresos medios-bajos, con un rango razonable (CLP-like en miles, o USD medios)
    ingreso = np.random.lognormal(mean=7.6, sigma=0.45)
    return float(np.clip(ingreso, 350, 12000))


def generar_usuarios(n):
    usuarios = []
    for _ in range(n):
        usuario_id = f"U{uuid.uuid4().hex[:8]}"
        edad = int(np.clip(np.random.normal(35, 10), 18, 70))
        ingreso = round(generar_ingreso(), 2)
        # Endeudamiento correlacionado levemente con menor ingreso (ruido incluido)
        endeudamiento = np.clip(
            np.random.normal(30 - (ingreso / 12000) * 15, 12), 0, 85
        )
        frecuencia_ahorro = np.random.choice(
            FREC_AHORRO, p=[0.35, 0.40, 0.25]
        )
        usuarios.append({
            "usuario_id": usuario_id,
            "edad": edad,
            "ingreso_mensual": ingreso,
            "nivel_endeudamiento": round(float(endeudamiento), 1),
            "frecuencia_ahorro": frecuencia_ahorro,
        })
    return pd.DataFrame(usuarios)


def generar_transacciones(usuarios_df):
    filas = []
    tx_id = 0
    for _, u in usuarios_df.iterrows():
        # Más transacciones si el ingreso es mayor (más actividad financiera)
        n_tx_mes = int(np.clip(np.random.normal(14 + u["ingreso_mensual"] / 900, 4), 5, 40))

        # Pesos de categoría levemente personalizados por usuario (ruido sobre los pesos base)
        pesos_usuario = PESOS_BASE * np.random.uniform(0.7, 1.3, size=len(CATS))
        pesos_usuario = pesos_usuario / pesos_usuario.sum()

        for mes_offset in range(MESES_HISTORIAL):
            mes_fecha_base = FECHA_FIN - timedelta(days=30 * mes_offset)
            for _ in range(n_tx_mes):
                categoria = np.random.choice(CATS, p=pesos_usuario)
                info = CATEGORIAS[categoria]
                comercio = np.random.choice(info["comercios"])
                low, high = info["rango_valor"]
                valor = round(float(np.random.uniform(low, high)), 2)
                dia_offset = np.random.randint(0, 28)
                fecha = mes_fecha_base - timedelta(days=dia_offset)

                tx_id += 1
                filas.append({
                    "transaccion_id": f"T{tx_id:07d}",
                    "usuario_id": u["usuario_id"],
                    "fecha": fecha.strftime("%Y-%m-%d"),
                    "descripcion": comercio,
                    "categoria": categoria,
                    "valor": valor,
                })
    return pd.DataFrame(filas)


def calcular_perfil_financiero(usuarios_df, transacciones_df):
    gasto_total = transacciones_df.groupby("usuario_id")["valor"].sum() / MESES_HISTORIAL
    df = usuarios_df.merge(gasto_total.rename("gasto_mensual_promedio"), on="usuario_id", how="left")

    ratio_gasto_ingreso = df["gasto_mensual_promedio"] / df["ingreso_mensual"]
    ahorro_score = df["frecuencia_ahorro"].map(FREC_AHORRO_SCORE)
    endeudamiento_norm = df["nivel_endeudamiento"] / 100.0

    # Puntaje de salud financiera: mayor es mejor
    salud_score = (
        (1 - endeudamiento_norm) * 0.4
        + ahorro_score * 0.35
        + (1 - np.clip(ratio_gasto_ingreso, 0, 1.2)) * 0.25
    )
    # Ruido controlado para que no sea una regla perfectamente separable
    salud_score += np.random.normal(0, 0.06, size=len(df))
    salud_score = np.clip(salud_score, 0, 1)

    perfil = pd.cut(
        salud_score,
        bins=[-0.01, 0.33, 0.52, 1.01],
        labels=["En riesgo", "En observación", "Saludable"],
    )
    df["perfil_financiero"] = perfil.astype(str)
    df["salud_score"] = round(salud_score, 3)
    return df.drop(columns=["gasto_mensual_promedio"]), df[["usuario_id", "gasto_mensual_promedio" if "gasto_mensual_promedio" in df.columns else "salud_score"]]


if __name__ == "__main__":
    usuarios_df = generar_usuarios(N_USUARIOS)
    transacciones_df = generar_transacciones(usuarios_df)
    usuarios_final_df, _ = calcular_perfil_financiero(usuarios_df, transacciones_df)

    usuarios_final_df.to_csv("usuarios.csv", index=False)
    transacciones_df.to_csv("transacciones.csv", index=False)

    print("Usuarios:", usuarios_final_df.shape)
    print("Transacciones:", transacciones_df.shape)
    print("\nDistribución de perfil_financiero:")
    print(usuarios_final_df["perfil_financiero"].value_counts(normalize=True).round(3))
    print("\nMuestra usuarios:")
    print(usuarios_final_df.head(3).to_string(index=False))
    print("\nMuestra transacciones:")
    print(transacciones_df.head(5).to_string(index=False))
