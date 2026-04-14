from __future__ import annotations

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException

from .model import load_model, predict, _artifact
from .schemas import ClaimScoreRequest, ClaimRiskResponse, HealthResponse

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")

PORT = 5123


@asynccontextmanager
async def lifespan(app: FastAPI):
    load_model()
    yield


app = FastAPI(
    title="Claim Risk Scoring Service",
    version="1.0.0",
    description="Microservice that scores insurance claims for fraud/error risk and provides explainable reasons.",
    lifespan=lifespan,
)


@app.get("/health", response_model=HealthResponse)
def health():
    return HealthResponse(status="up", model_loaded=_artifact is not None)


@app.post("/score", response_model=ClaimRiskResponse)
def score_claim(request: ClaimScoreRequest):
    try:
        result = predict(request)
    except RuntimeError as exc:
        raise HTTPException(status_code=503, detail=str(exc))
    return ClaimRiskResponse(**result)


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host="127.0.0.1", port=PORT, reload=True)
