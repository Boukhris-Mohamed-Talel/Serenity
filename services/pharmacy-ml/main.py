import os
from datetime import datetime
from typing import Optional

from fastapi import FastAPI, Header, HTTPException
from pydantic import BaseModel
from forecast_pipeline import run_forecast_pipeline


app = FastAPI(title="Pharmacy ML Service", version="1.0.0")
INTERNAL_API_KEY = os.getenv("PHARMACY_ML_INTERNAL_API_KEY", "serenity-internal-key-dev")


class ForecastRunRequest(BaseModel):
    pharmacyId: Optional[int] = None


def _ensure_internal_key(x_internal_key: Optional[str]) -> None:
    if x_internal_key != INTERNAL_API_KEY:
        raise HTTPException(status_code=401, detail="Unauthorized internal key")


@app.get("/internal/health")
def healthcheck() -> dict:
    return {"status": "ok"}


@app.post("/internal/forecast/run")
def run_forecast(
    request: Optional[ForecastRunRequest] = None,
    x_internal_key: Optional[str] = Header(default=None, alias="X-Internal-Key"),
) -> dict:
    _ensure_internal_key(x_internal_key)
    pharmacy_id = request.pharmacyId if request is not None else None
    result = run_forecast_pipeline(pharmacy_id)
    return {"runAt": datetime.now().isoformat(), **result}
