# Pharmacy ML Service

FastAPI microservice for stock demand forecasting using `StatsForecast + TSB`.

## Run

```bash
cd services/pharmacy-ml
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8096 --reload
```

## Environment Variables

- `PHARMACY_ML_DB_URL` (default: `mysql+pymysql://root:@localhost:3306/pharmacy_db`)
- `PHARMACY_ML_INTERNAL_API_KEY` (default: `serenity-internal-key-dev`)
- `PHARMACY_ML_LOOKBACK_DAYS` (default: `180`)
- `PHARMACY_ML_HORIZON_DAYS` (default: `14`)
- `PHARMACY_ML_SHORT_HISTORY_DAYS` (default: `21`)
- `PHARMACY_ML_TSB_ALPHA_D` (default: `0.3`)
- `PHARMACY_ML_TSB_ALPHA_P` (default: `0.3`)

## Endpoints

- `GET /internal/health`
- `POST /internal/forecast/run` with header `X-Internal-Key`
  - optional body: `{ "pharmacyId": 123 }`
