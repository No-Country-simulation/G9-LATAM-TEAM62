"""
Entrenamiento del clasificador de categorías (Nivel 3 del pipeline híbrido).

Fuentes de datos combinadas:
  1. category_mappings + category_keywords reales de Backend (schema.sql) --
     cientos de comercios chilenos reales, ya en producción.
  2. Comercios de nuestro dataset simulado (generar_dataset.py) -- variedad
     adicional, mapeados a las categorías reales de Category.java.
  3. Aumentación con prefijos/sufijos realistas ("COMPRA X", "PAGO X",
     "X PROVIDENCIA") -- para que el modelo generalice a texto con ruido,
     no solo al nombre limpio del comercio.

Preprocesamiento: replica EXACTA de TextNormalizer.java (mayúsculas, sin
tildes, sin números/códigos, solo letras y espacios) -- así el modelo ve en
entrenamiento la misma distribución de texto que va a ver en producción.
"""

import json
import re
import unicodedata
import random

import numpy as np
import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report, confusion_matrix, f1_score
from sklearn.pipeline import Pipeline
import joblib

random.seed(42)
np.random.seed(42)


# ---------------------------------------------------------------------------
# 1. Normalización -- replica exacta de TextNormalizer.java
# ---------------------------------------------------------------------------
_NUMBERS_AND_CODES = re.compile(r"[0-9]+([.\-/][0-9kK]+)*")
_NON_ALPHA = re.compile(r"[^A-Z\s]")
_MULTI_SPACE = re.compile(r"\s{2,}")


def normalize(text: str) -> str:
    if not text:
        return ""
    result = text.upper()
    result = unicodedata.normalize("NFD", result)
    result = "".join(c for c in result if unicodedata.category(c) != "Mn")  # quita diacríticos
    result = _NUMBERS_AND_CODES.sub("", result)
    result = _NON_ALPHA.sub(" ", result)
    result = _MULTI_SPACE.sub(" ", result).strip()
    return result


# ---------------------------------------------------------------------------
# 2. Fuente 1: category_mappings + category_keywords reales de Backend
# ---------------------------------------------------------------------------
with open("fuente_backend.json", encoding="utf-8") as f:
    fuente_backend = json.load(f)

pares_backend = [(desc, cat) for desc, cat in fuente_backend["mappings"]]
pares_backend += [(kw, cat) for kw, cat in fuente_backend["keywords"]]

# ---------------------------------------------------------------------------
# 3. Fuente 2: comercios de nuestro dataset simulado, mapeados a Category.java
# ---------------------------------------------------------------------------
COMERCIOS_SIMULADOS = {
    "FOOD": ["Supermercado Lider", "Jumbo", "Feria libre", "Restaurante El Fogon",
             "Panaderia San Jose", "Almacen de barrio", "Delivery Rappi Comida"],
    "TRANSPORT": ["Copec Combustible", "Uber", "Metro tarjeta BIP", "Estacionamiento Centro",
                  "Taller mecanico", "Peaje autopista"],
    "HEALTH": ["Farmacia Cruz Verde", "Clinica Santa Maria", "Consulta dental",
               "Seguro complementario", "Optica Rotter"],
    "HOUSING": ["Arriendo departamento", "Gastos comunes", "Ferreteria Sodimac",
                "Reparacion hogar"],
    "EDUCATION": ["Matricula colegio", "Curso online Alura", "Libreria Nacional",
                  "Universidad mensualidad"],
    "ENTERTAINMENT": ["Streaming Netflix", "Streaming Spotify", "Cine Hoyts",
                       "Bar restaurante nocturno", "Viaje fin de semana", "Videojuegos Steam"],
    "UTILITIES": ["Cuenta de luz", "Cuenta de agua", "Internet y telefonia"],
}
pares_simulados = [(nombre, cat) for cat, nombres in COMERCIOS_SIMULADOS.items() for nombre in nombres]

print(f"Pares de Backend (mappings + keywords): {len(pares_backend)}")
print(f"Pares del dataset simulado: {len(pares_simulados)}")


# ---------------------------------------------------------------------------
# 4. Aumentación: variantes realistas de cada comercio/palabra clave
# ---------------------------------------------------------------------------
PREFIJOS = ["", "COMPRA ", "PAGO ", "COMPRA NACIONAL ", "TEF A "]
SUFIJOS = ["", " SANTIAGO", " PROVIDENCIA", " LAS CONDES", " CHILE", " SPA"]


def aumentar(base: str, n: int = 4) -> list[str]:
    variantes = set()
    variantes.add(base)  # el término limpio también entra
    intentos = 0
    while len(variantes) < n + 1 and intentos < 20:
        intentos += 1
        pre = random.choice(PREFIJOS)
        suf = random.choice(SUFIJOS)
        variantes.add(f"{pre}{base}{suf}".strip())
    return list(variantes)


filas = []
for desc, cat in pares_backend + pares_simulados:
    for variante in aumentar(desc, n=4):
        texto_normalizado = normalize(variante)
        if texto_normalizado:
            filas.append({"texto": texto_normalizado, "categoria": cat})

df = pd.DataFrame(filas).drop_duplicates()
print(f"\nDataset final (tras normalizar y quitar duplicados): {len(df)} filas")
print(df["categoria"].value_counts())

df.to_csv("dataset_entrenamiento.csv", index=False)
