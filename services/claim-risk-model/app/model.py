from __future__ import annotations

import logging
from pathlib import Path

import joblib
import numpy as np
import pandas as pd

from .schemas import ClaimScoreRequest

logger = logging.getLogger(__name__)

MODEL_PATH = Path(__file__).resolve().parent.parent / "models" / "insurance_claim_risk_model.joblib"

_artifact: dict | None = None


def load_model() -> None:
    global _artifact
    if not MODEL_PATH.exists():
        logger.warning("Model file not found at %s – run the notebook first.", MODEL_PATH)
        return
    _artifact = joblib.load(MODEL_PATH)
    logger.info("Model loaded from %s", MODEL_PATH)

    imp_path = MODEL_PATH.parent / "insurance_claim_feature_importance.csv"
    if imp_path.exists():
        df = pd.read_csv(imp_path)
        for _, row in df.iterrows():
            _importance_weights[row["feature"]] = max(float(row["importance_mean"]), 0.0001)
        logger.info("Loaded %d feature importance weights", len(_importance_weights))


def _risk_band(score: float, thresholds: dict) -> str:
    if score >= thresholds["HIGH"][0]:
        return "HIGH"
    if score >= thresholds["MEDIUM"][0]:
        return "MEDIUM"
    return "LOW"


_importance_weights: dict[str, float] = {}


def _top_reasons(row: dict) -> list[str]:
    w = _importance_weights or {k: 0.1 for k in row}
    reasons: list[tuple[str, float]] = []

    days_since_last_claim = row.get("days_since_last_claim")
    if days_since_last_claim is None:
        days_since_last_claim = 999

    if row.get("ocr_severity", 0) >= 3:
        reasons.append(("Major OCR mismatches detected", w.get("ocr_severity", 0.1) * row["ocr_severity"]))
    if row.get("ocr_mismatch_count", 0) >= 2:
        reasons.append(("Multiple OCR mismatches", w.get("ocr_mismatch_count", 0.1) * row["ocr_mismatch_count"]))
    if row.get("amount_discrepancy", 0) > 0.10:
        reasons.append(("Claimed amount differs from OCR extraction", w.get("amount_discrepancy", 0.1) * row["amount_discrepancy"]))
    if row.get("user_claims_30d", 0) > 3:
        reasons.append(("High claim frequency (30d)", w.get("user_claims_30d", 0.1) * row["user_claims_30d"]))
    if row.get("user_rejected_claims", 0) > 2:
        reasons.append(("History of rejected claims", w.get("user_rejected_claims", 0.1) * row["user_rejected_claims"]))
    if days_since_last_claim < 3:
        reasons.append(("Very recent previous claim", w.get("days_since_last_claim", 0.1) * (5 - days_since_last_claim)))
    if row.get("high_amount_flag", 0) == 1:
        reasons.append(("Unusually high claim amount", w.get("high_amount_flag", 0.1)))
    if row.get("reimbursement_ratio", 0) > 0.95:
        reasons.append(("Near-full reimbursement ratio", w.get("reimbursement_ratio", 0.1) * row["reimbursement_ratio"]))
    ocr_dec = row.get("ocr_decision", "NONE")
    if ocr_dec == "MAJOR_BLOCKED":
        reasons.append(("OCR flagged as MAJOR_BLOCKED", w.get("ocr_decision", 0.1) * 3))
    elif ocr_dec == "NONE":
        reasons.append(("No OCR analysis performed yet", w.get("ocr_decision", 0.05)))
    if row.get("file_count", 2) == 0:
        reasons.append(("No supporting documents attached", w.get("file_count", 0.1)))

    if not reasons:
        reasons = [("No strong risk anomalies detected", 0.0)]

    reasons.sort(key=lambda x: x[1], reverse=True)
    return [r[0] for r in reasons[:3]]


def predict(req: ClaimScoreRequest) -> dict:
    if _artifact is None:
        raise RuntimeError("Model not loaded. Run the notebook first to generate the .joblib artifact.")

    pipeline = _artifact["pipeline"]
    feature_cols = _artifact["feature_columns"]
    thresholds = _artifact["risk_band_thresholds"]

    raw = req.model_dump()

    # Map API field names and compute engineered features
    raw["ocr_decision"] = raw.get("ocr_decision") or "NONE"
    raw["status"] = "SUBMITTED"

    ocr_ext = raw.get("ocr_extracted_amount") or 0
    raw["ocr_submitted_amount"] = raw.get("ocr_submitted_amount") or raw["amount"]
    raw["ocr_extracted_amount"] = ocr_ext

    raw["amount_discrepancy"] = (
        abs(raw["amount"] - ocr_ext) / ocr_ext if ocr_ext > 0 else 0.0
    )
    raw["reimbursement_ratio"] = (
        raw["reimbursement_amount"] / raw["amount"] if raw["amount"] > 0 else 1.0
    )
    raw["high_amount_flag"] = 0
    raw["frequent_claimant_flag"] = int(raw.get("user_claims_30d", 0) > 3)
    raw["has_rejections_flag"] = int(raw.get("user_rejected_claims", 0) > 0)
    raw["ocr_severity"] = raw.get("ocr_major_count", 0) * 3 + raw.get("ocr_minor_count", 0)

    df = pd.DataFrame([raw])[feature_cols]
    prob = float(pipeline.predict_proba(df)[:, 1][0])
    score = round(prob * 100, 1)
    band = _risk_band(score, thresholds)
    reasons = _top_reasons(raw)

    return {"risk_score": score, "risk_band": band, "top_reasons": reasons}
