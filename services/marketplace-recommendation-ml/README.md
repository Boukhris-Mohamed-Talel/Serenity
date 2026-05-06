# Marketplace recommendation ML (Olist collaborative filtering)

Trains a **matrix factorization** model (**`TruncatedSVD`**) on the public [Brazilian E-Commerce (Olist) Kaggle dataset](https://www.kaggle.com/datasets/olistbr/brazilian-ecommerce): users = `customer_unique_id`, items = `product_id`, values = purchase counts on **delivered** orders.

Serves HTTP recommendations via **FastAPI** (`POST /v1/recommend`).

> This is **off-the-shelf Olist product IDs**, not your Java marketplace SKUs. Use it as a **standalone ML service** / coursework artifact, or wire the optional Spring proxy for demos.

## Jupyter notebook (what you run day-to-day)

Open and run top-to-bottom:

- [`Olist_Recommender_Train_Eval.ipynb`](./Olist_Recommender_Train_Eval.ipynb)

The notebook installs deps, optionally pulls data via `kagglehub`, runs **clean → evaluate → train**, writes `artifacts/`, and prints a sample recommendation.

**Cursor / kernel picker:** nested `.venv` folders are often missing from the list. Open the repo-root workspace **[`marketplace-ml.code-workspace`](../marketplace-ml.code-workspace)** (two roots: whole repo + this service) so this folder’s `.venv` shows up, or use **Select Kernel → Enter interpreter path** to `services\marketplace-recommendation-ml\.venv\Scripts\python.exe`. CLI check: `.\.venv\Scripts\python -m jupyter kernelspec list` should include `marketplace-recommendation-ml`.

## Quick start (synthetic smoke test — no Kaggle)

```bash
cd services/marketplace-recommendation-ml
python -m venv .venv
.venv\Scripts\activate          # Windows
pip install -r requirements.txt
pytest -q
```

## Train on real Kaggle data

1. Download the dataset from Kaggle and unzip the CSVs.
2. Point `--data-dir` at the folder that contains `olist_orders_dataset.csv`, etc. (often the zip root).

```bash
pip install -r requirements.txt
python train.py --data-dir "C:\path\to\unzipped\olist\folder"
```

Optional: install [`kagglehub`](https://github.com/Kaggle/kagglehub), run `kaggle login`, then:

```bash
python train.py --try-kaggle-download
```

Artifacts:

- `artifacts/model.joblib` — trained `StoredPurchaseModel`
- `artifacts/model_meta.json` — row/column counts

## Run the API

```bash
# After training (default model path)
set MODEL_PATH=artifacts\model.joblib
uvicorn serve:app --host 127.0.0.1 --port 8095
```

`POST http://127.0.0.1:8095/v1/recommend` JSON body:

```json
{
  "customer_unique_id": "some-uuid-from-olist-customers",
  "top_k": 10,
  "mask_purchased": true
}
```

`GET /health` — readiness.

## Docker

Build from this directory:

```bash
docker build -t marketplace-olist-ml .
docker run --rm -p 8095:8095 -v "%CD%\artifacts:/app/artifacts" marketplace-olist-ml
```

## Optional: Spring marketplace-service proxy

1. Run this ML API on port **8095** (see above).
2. Start `marketplace-service` with env  
   `ML_OLIST_BASE_URL=http://127.0.0.1:8095`  
   (or set `app.ml.olist-base-url` in `application.yml`).
3. Call (no JWT required for this route):

`POST http://localhost:8088/api/articles/recommendations/olist`  
`Content-Type: application/json`

```json
{
  "customerUniqueId": "paste-a-customer_unique_id-from-olist_customers_dataset",
  "topK": 10
}
```

If `ML_OLIST_BASE_URL` is empty or the Python service is down, the endpoint returns **503**.
