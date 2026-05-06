"""End-to-end pipeline on tiny synthetic Olist-shaped CSVs (no Kaggle)."""

from __future__ import annotations

from pathlib import Path

import joblib
import pandas as pd

from recommender.pipeline import run_pipeline
from recommender.train_matrix import StoredPurchaseModel


def _write_minimal_olist(tmp: Path) -> None:
    customers = pd.DataFrame(
        {
            "customer_id": ["c1", "c2"],
            "customer_unique_id": ["u1", "u2"],
            "customer_zip_code_prefix": [1, 2],
            "customer_city": ["a", "b"],
            "customer_state": ["SP", "RJ"],
        }
    )
    # Spread timestamps so temporal split yields train + test line items
    orders = pd.DataFrame(
        {
            "order_id": [f"o{i}" for i in range(60)],
            "customer_id": ["c1" if i % 2 == 0 else "c2" for i in range(60)],
            "order_status": ["delivered"] * 60,
            "order_purchase_timestamp": [f"2017-01-{1 + (i % 28):02d}T12:00:00Z" for i in range(60)],
            "order_approved_at": ["2017-01-02"] * 60,
            "order_delivered_carrier_date": ["2017-01-03"] * 60,
            "order_delivered_customer_date": ["2017-01-04"] * 60,
            "order_estimated_delivery_date": ["2017-01-05"] * 60,
        }
    )
    items = pd.DataFrame(
        {
            "order_id": [f"o{i}" for i in range(60)],
            "order_item_id": list(range(1, 61)),
            "product_id": [f"p{1 + (i % 5)}" for i in range(60)],
            "seller_id": ["s"] * 60,
            "shipping_limit_date": ["2017-01-01"] * 60,
            "price": [10.0 + i for i in range(60)],
            "freight_value": [1.0] * 60,
        }
    )
    products = pd.DataFrame(
        {
            "product_id": [f"p{i}" for i in range(1, 6)],
            "product_category_name": [f"cat_{i}" for i in range(1, 6)],
            "product_name_lenght": [1] * 5,
            "product_description_lenght": [1] * 5,
            "product_photos_qty": [1] * 5,
            "product_weight_g": [1] * 5,
            "product_length_cm": [1] * 5,
            "product_height_cm": [1] * 5,
            "product_width_cm": [1] * 5,
        }
    )
    tmp.mkdir(parents=True, exist_ok=True)
    customers.to_csv(tmp / "olist_customers_dataset.csv", index=False)
    orders.to_csv(tmp / "olist_orders_dataset.csv", index=False)
    items.to_csv(tmp / "olist_order_items_dataset.csv", index=False)
    products.to_csv(tmp / "olist_products_dataset.csv", index=False)


def test_pipeline_artifacts_and_recommend(tmp_path: Path) -> None:
    data = tmp_path / "olist"
    art = tmp_path / "out"
    _write_minimal_olist(data)
    run_pipeline(
        data_dir=data,
        artifacts_dir=art,
        n_components=3,
        min_user_purchases=1,
        min_product_purchases=1,
        test_fraction=0.2,
        eval_k=5,
        eval_max_users=500,
        skip_eval=False,
    )
    assert (art / "model.joblib").exists()
    assert (art / "metrics.json").exists()
    assert (art / "data_profile.json").exists()

    m: StoredPurchaseModel = joblib.load(art / "model.joblib")
    rec = m.recommend("u1", top_k=3, mask_purchased=True)
    assert len(rec) >= 1
    cold = m.recommend("unknown_user", top_k=2, mask_purchased=False)
    assert len(cold) == 2
