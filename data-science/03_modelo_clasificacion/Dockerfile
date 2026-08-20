FROM python:3.12-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY modelo_clasificador.joblib servicio_inferencia.py ./
EXPOSE 8000
CMD ["uvicorn", "servicio_inferencia:app", "--host", "0.0.0.0", "--port", "8000"]
