"""
FastAPI inference for the trained Olist recommender.

Run from this directory:
  uvicorn serve:app --host 0.0.0.0 --port 8095

Env:
  MODEL_PATH  (default: artifacts/model.joblib)
"""

from __future__ import annotations

import os
from contextlib import asynccontextmanager
from pathlib import Path
from typing import Any

import joblib
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

from recommender.train_matrix import StoredPurchaseModel

MODEL_PATH = Path(os.environ.get("MODEL_PATH", "artifacts/model.joblib")).resolve()
_model: StoredPurchaseModel | None = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global _model
    if not MODEL_PATH.exists():
        raise RuntimeError(
            f"Model not found at {MODEL_PATH}. Train first: python train.py --data-dir <path-to-olist-csvs>"
        )
    _model = joblib.load(MODEL_PATH)
    yield
    _model = None


app = FastAPI(title="Marketplace Olist recommender", version="1.0.0", lifespan=lifespan)


class RecommendRequest(BaseModel):
    customer_unique_id: str = Field(..., min_length=1, max_length=128)
    top_k: int = Field(10, ge=1, le=50)
    mask_purchased: bool = Field(True, description="Hide items the user already bought in training data")


class RecommendItem(BaseModel):
    product_id: str
    score: float
    category: str = ""


class RecommendResponse(BaseModel):
    customer_unique_id: str
    items: list[RecommendItem]
    cold_start: bool


@app.get("/health")
def health() -> dict[str, Any]:
    return {"status": "ok", "model_loaded": _model is not None, "model_path": str(MODEL_PATH)}


@app.post("/v1/recommend", response_model=RecommendResponse)
def recommend(body: RecommendRequest) -> RecommendResponse:
    if _model is None:
        raise HTTPException(503, "Model not loaded")
    uid = body.customer_unique_id.strip()
    cold = uid not in _model.uid_index
    raw = _model.recommend(uid, body.top_k, body.mask_purchased)
    items = [
        RecommendItem(
            product_id=pid,
            score=sc,
            category=_model.product_categories.get(pid, "") or _model.product_titles.get(pid, ""),
        )
        for pid, sc in raw
    ]
    return RecommendResponse(customer_unique_id=uid, items=items, cold_start=cold)
