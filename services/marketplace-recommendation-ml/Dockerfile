FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY recommender ./recommender
COPY serve.py .

# Expect model mounted at runtime, e.g.:
#   docker run -p 8095:8095 -v $(pwd)/artifacts:/app/artifacts marketplace-ml
ENV MODEL_PATH=/app/artifacts/model.joblib

EXPOSE 8095
CMD ["uvicorn", "serve:app", "--host", "0.0.0.0", "--port", "8095"]
