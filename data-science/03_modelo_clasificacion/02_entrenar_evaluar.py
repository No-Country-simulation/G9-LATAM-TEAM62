"""
Entrena el clasificador (TF-IDF + Regresión Logística), lo evalúa, y lo
serializa. Elegí TF-IDF + LogisticRegression sobre alternativas más
complejas (embeddings, redes neuronales) a propósito: para texto corto de
comercios, es un enfoque estándar y muy efectivo, entrena en segundos,
pesa poco (importa para el contenedor), y da probabilidades calibradas de
forma nativa -- justo lo que necesita el contrato de confianza con Backend.
"""

import json
import joblib
import numpy as np
import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report, confusion_matrix, f1_score, accuracy_score
from sklearn.pipeline import Pipeline

df = pd.read_csv("dataset_entrenamiento.csv")

X_train, X_test, y_train, y_test = train_test_split(
    df["texto"], df["categoria"],
    test_size=0.2, random_state=42, stratify=df["categoria"],
)

pipeline = Pipeline([
    ("tfidf", TfidfVectorizer(ngram_range=(1, 2), min_df=1, sublinear_tf=True)),
    ("clf", LogisticRegression(max_iter=1000, C=5.0, class_weight="balanced")),
])

pipeline.fit(X_train, y_train)

# ---------------------------------------------------------------------------
# Evaluación (Bloque 2, ítem 1 -- parte del entregable obligatorio del brief)
# ---------------------------------------------------------------------------
y_pred = pipeline.predict(X_test)

acc = accuracy_score(y_test, y_pred)
f1_macro = f1_score(y_test, y_pred, average="macro")
f1_weighted = f1_score(y_test, y_pred, average="weighted")

reporte = classification_report(y_test, y_pred, output_dict=True, zero_division=0)
reporte_texto = classification_report(y_test, y_pred, zero_division=0)

categorias_ordenadas = sorted(df["categoria"].unique())
matriz = confusion_matrix(y_test, y_pred, labels=categorias_ordenadas)

print("=" * 70)
print(f"Accuracy global:      {acc:.3f}")
print(f"F1 macro (sin ponderar por tamaño de clase): {f1_macro:.3f}")
print(f"F1 weighted (ponderado por tamaño de clase): {f1_weighted:.3f}")
print("=" * 70)
print(reporte_texto)

# Guardar el reporte completo para el .md de evaluación
with open("reporte_evaluacion.json", "w", encoding="utf-8") as f:
    json.dump({
        "accuracy": acc,
        "f1_macro": f1_macro,
        "f1_weighted": f1_weighted,
        "por_categoria": reporte,
        "matriz_confusion": matriz.tolist(),
        "categorias": categorias_ordenadas,
        "n_train": len(X_train),
        "n_test": len(X_test),
    }, f, ensure_ascii=False, indent=2)

# ---------------------------------------------------------------------------
# Serialización (entregable obligatorio del brief)
# ---------------------------------------------------------------------------
joblib.dump(pipeline, "modelo_clasificador.joblib")
print("\nModelo serializado en modelo_clasificador.joblib")

# Prueba rápida con ejemplos que el modelo NUNCA vio (ni como base, ni como
# variante aumentada) -- para confirmar que generaliza y no memorizó.
import sys
sys.path.insert(0, ".")
from importlib import import_module
_construir = import_module("01_construir_dataset")
normalize = _construir.normalize

ejemplos_nuevos = [
    "COMPRA STARBUCKS COFFEE LAS CONDES",
    "PAGO TARJETA CREDITO VISA",
    "UBER TRIP SANTIAGO CENTRO",
    "NETFLIX COM SUSCRIPCION MENSUAL",
    "FARMACIA AHUMADA VITACURA",
]
print("\n--- Prueba con ejemplos nuevos (fuera del set de entrenamiento) ---")
for ejemplo in ejemplos_nuevos:
    texto_norm = normalize(ejemplo)
    pred = pipeline.predict([texto_norm])[0]
    proba = pipeline.predict_proba([texto_norm]).max()
    print(f"  {ejemplo!r:50s} -> {texto_norm!r:35s} => {pred} (confianza {proba:.2f})")

