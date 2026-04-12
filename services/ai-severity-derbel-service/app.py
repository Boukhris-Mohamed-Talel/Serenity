"""
============================================================
  FLASK API — Medical Severity Prediction Service
  Endpoint: POST /predict
  Port: 5001
============================================================
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
import joblib
import os

app = Flask(__name__)
CORS(app)

# ═══════════════════════════════════════════════════════════
#  LOAD MODEL AT STARTUP
# ═══════════════════════════════════════════════════════════

MODEL_DIR = "model"

try:
    model = joblib.load(os.path.join(MODEL_DIR, "severity_model.pkl"))
    tfidf = joblib.load(os.path.join(MODEL_DIR, "tfidf_vectorizer.pkl"))
    label_encoder = joblib.load(os.path.join(MODEL_DIR, "label_encoder.pkl"))
    print("Model, TF-IDF vectorizer, and label encoder loaded successfully.")
except FileNotFoundError:
    print("Model files not found! Run 'python train_model.py' first.")
    model = None
    tfidf = None
    label_encoder = None


# ═══════════════════════════════════════════════════════════
#  PREDICTION ENDPOINT
# ═══════════════════════════════════════════════════════════

@app.route("/predict", methods=["POST"])
def predict():
    """
    Predict the severity of a medical diagnosis.

    Request body (JSON):
        {"diagnosis": "G43.9 - Migraine, unspecified"}

    Response (JSON):
        {
            "severity": "MEDIUM",
            "confidence": 0.87,
            "probabilities": {"LOW": 0.05, "MEDIUM": 0.87, "HIGH": 0.08}
        }
    """
    if model is None:
        return jsonify({"error": "Model not loaded. Train the model first."}), 503

    data = request.get_json()
    if not data or "diagnosis" not in data:
        return jsonify({"error": "Missing 'diagnosis' field in request body."}), 400

    diagnosis_text = data["diagnosis"].strip().lower()
    if not diagnosis_text:
        return jsonify({"error": "'diagnosis' field cannot be empty."}), 400

    # Vectorize the input
    X = tfidf.transform([diagnosis_text])

    # Predict class
    prediction_encoded = model.predict(X)[0]
    severity = label_encoder.inverse_transform([prediction_encoded])[0]

    # Get probabilities (if the model supports it)
    probabilities = {}
    try:
        proba = model.predict_proba(X)[0]
        for idx, label in enumerate(label_encoder.classes_):
            probabilities[label] = round(float(proba[idx]), 4)
        confidence = round(float(max(proba)), 4)
    except AttributeError:
        confidence = 1.0
        probabilities = {severity: 1.0}

    return jsonify({
        "severity": severity,
        "confidence": confidence,
        "probabilities": probabilities,
    })


# ═══════════════════════════════════════════════════════════
#  HEALTH CHECK
# ═══════════════════════════════════════════════════════════

@app.route("/health", methods=["GET"])
def health():
    return jsonify({
        "status": "UP",
        "model_loaded": model is not None,
        "service": "ai-severity-derbel-service",
    })


# ═══════════════════════════════════════════════════════════
#  RUN SERVER
# ═══════════════════════════════════════════════════════════

if __name__ == "__main__":
    print("\nAI Severity Prediction Service starting on port 5001...")
    app.run(host="0.0.0.0", port=5001, debug=True)
