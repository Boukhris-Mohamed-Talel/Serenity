from __future__ import annotations

from pathlib import Path

import pandas as pd

from recommender.cleaning import clean_olist, line_items_to_interaction_counts


def test_clean_olist_minimal(tmp_path: Path) -> None:
    d = tmp_path / "d"
    d.mkdir()
    pd.DataFrame(
        {
            "customer_id": ["c1"],
            "customer_unique_id": ["u1"],
            "customer_zip_code_prefix": [1],
            "customer_city": ["x"],
            "customer_state": ["SP"],
        }
    ).to_csv(d / "olist_customers_dataset.csv", index=False)
    pd.DataFrame(
        {
            "order_id": ["o1"],
            "customer_id": ["c1"],
            "order_status": ["delivered"],
            "order_purchase_timestamp": ["2018-05-01T10:00:00Z"],
            "order_approved_at": ["2018-05-02"],
            "order_delivered_carrier_date": ["2018-05-03"],
            "order_delivered_customer_date": ["2018-05-04"],
            "order_estimated_delivery_date": ["2018-05-06"],
        }
    ).to_csv(d / "olist_orders_dataset.csv", index=False)
    pd.DataFrame(
        {
            "order_id": ["o1"],
            "order_item_id": [1],
            "product_id": ["p9"],
            "seller_id": ["s"],
            "shipping_limit_date": ["2018-05-01"],
            "price": [12.5],
            "freight_value": [1.0],
        }
    ).to_csv(d / "olist_order_items_dataset.csv", index=False)
    pd.DataFrame(
        {
            "product_id": ["p9"],
            "product_category_name": ["books"],
            "product_name_lenght": [1],
            "product_description_lenght": [1],
            "product_photos_qty": [1],
            "product_weight_g": [1],
            "product_length_cm": [1],
            "product_height_cm": [1],
            "product_width_cm": [1],
        }
    ).to_csv(d / "olist_products_dataset.csv", index=False)

    c = clean_olist(d)
    assert len(c.line_items) == 1
    inter = line_items_to_interaction_counts(c.line_items)
    assert inter.iloc[0]["purchase_count"] == 1
    assert "clean_rows" in c.profile
