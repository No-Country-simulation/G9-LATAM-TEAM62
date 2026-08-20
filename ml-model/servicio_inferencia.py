"""
Servicio de inferencia del clasificador de categorías (Nivel 3 del pipeline
híbrido de Backend). Expone exactamente el contrato que MlInferenceService.java
espera:

    POST /predict
    Body:     {"descripcion": "COMPRA JUMBO PROVIDENCIA"}
    Response: {"category": "FOOD", "confidence": 0.93}

Config esperada del lado de Backend (application.properties):
    ml.inference.enabled=true
    ml.inference.url=http://localhost:8000/predict
    ml.inference.min-confidence=0.60

Nota: la normalización de texto (mayúsculas, sin tildes, sin números) ya la
aplica Backend antes de llamar acá (ver TextNormalizer.java) -- este servicio
la vuelve a aplicar de forma defensiva, por si se lo invoca directo sin pasar
por Backend (pruebas, otro cliente futuro).
"""

import re
import unicodedata
from pathlib import Path

import joblib
from fastapi import FastAPI
from pydantic import BaseModel

MODEL_PATH = Path(__file__).parent / "modelo_clasificador.joblib"

app = FastAPI(
    title="Finance AI — Servicio de Clasificación (Nivel 3)",
    description="Modelo de ML para clasificar transacciones por descripción. "
                "Consumido por CategoryClassifierService.java vía MlInferenceService.",
    version="1.0.0",
)

_pipeline = joblib.load(MODEL_PATH)

_NUMBERS_AND_CODES = re.compile(r"[0-9]+([.\-/][0-9kK]+)*")
_NON_ALPHA = re.compile(r"[^A-Z\s]")
_MULTI_SPACE = re.compile(r"\s{2,}")


def normalize(text: str) -> str:
    """Réplica exacta de TextNormalizer.java -- ver docs/contrato-ingesta-cartolas.md."""
    if not text:
        return ""
    result = text.upper()
    result = unicodedata.normalize("NFD", result)
    result = "".join(c for c in result if unicodedata.category(c) != "Mn")
    result = _NUMBERS_AND_CODES.sub("", result)
    result = _NON_ALPHA.sub(" ", result)
    result = _MULTI_SPACE.sub(" ", result).strip()
    return result


class PredictRequest(BaseModel):
    descripcion: str


class PredictResponse(BaseModel):
    category: str
    confidence: float


@app.get("/health")
def health():
    """Chequeo simple para verificar que el servicio y el modelo están arriba."""
    return {"status": "ok", "model_loaded": _pipeline is not None}


@app.post("/predict", response_model=PredictResponse)
def predict(request: PredictRequest) -> PredictResponse:
    texto = normalize(request.descripcion)

    if not texto:
        # Texto vacío tras normalizar: no hay nada de qué inferir.
        # Confianza 0 -- Backend, con su umbral de 0.60, lo descarta y cae al fallback.
        return PredictResponse(category="OTHER_EXPENSE", confidence=0.0)

    categoria = _pipeline.predict([texto])[0]
    probabilidades = _pipeline.predict_proba([texto])[0]
    confianza = float(probabilidades.max())

    return PredictResponse(category=categoria, confidence=round(confianza, 4))


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
