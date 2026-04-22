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
- `PHARMACY_ML_CNOPT_PROCESSOR_DIR` (default: `microsoft/layoutlmv3-base`)
- `PHARMACY_ML_CNOPT_NER_MODEL_DIR` (default: `layoutlmv3_cnopt_v4_ner_output/checkpoint-200`)
- `PHARMACY_ML_CNOPT_DOC_MODEL_DIR` (default: `layoutlmv3_cnopt_v4_doc_status_output/checkpoint-150`)
- `PHARMACY_ML_CNOPT_MAX_PDF_PAGES` (default: `1`)
- `PHARMACY_ML_CNOPT_PDF_DPI` (default: `150`)
- `PHARMACY_ML_CNOPT_IMAGE_MAX_SIDE` (default: `1800`)
- `PHARMACY_ML_CNOPT_MAX_OCR_TOKENS` (default: `220`)
- `PHARMACY_ML_CNOPT_TOKEN_MAX_LENGTH` (default: `256`)
- `PHARMACY_ML_CNOPT_OCR_LANG` (default: `en`)

## Endpoints

- `GET /internal/health`
- `POST /internal/forecast/run` with header `X-Internal-Key`
  - optional body: `{ "pharmacyId": 123 }`
- `POST /internal/pharmacy-applications/verify-cnopt` with header `X-Internal-Key`
  - multipart form fields:
    - `file` (required): PDF/PNG/JPG/JPEG
    - `filename` (optional): original filename override
