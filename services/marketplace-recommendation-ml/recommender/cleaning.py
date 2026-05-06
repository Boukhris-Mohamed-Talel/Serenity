"""
Load and clean Brazilian E-Commerce (Olist) CSVs for recommender training.

Outputs structured tables + optional profile JSON for reproducibility.
"""

from __future__ import annotations

import json
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

import numpy as np
import pandas as pd


@dataclass
class CleanedOlist:
    """Cleaned, joined line-level data (one row per order line item)."""

    line_items: pd.DataFrame
    products: pd.DataFrame
    profile: dict[str, Any] = field(default_factory=dict)


def load_raw_csvs(data_dir: Path) -> tuple[pd.DataFrame, pd.DataFrame, pd.DataFrame, pd.DataFrame]:
    req = [
        "olist_customers_dataset.csv",
        "olist_orders_dataset.csv",
        "olist_order_items_dataset.csv",
        "olist_products_dataset.csv",
    ]
    missing = [f for f in req if not (data_dir / f).exists()]
    if missing:
        raise FileNotFoundError(f"Missing in {data_dir}: {missing}")
    customers = pd.read_csv(data_dir / "olist_customers_dataset.csv", low_memory=False)
    orders = pd.read_csv(data_dir / "olist_orders_dataset.csv", low_memory=False)
    items = pd.read_csv(data_dir / "olist_order_items_dataset.csv", low_memory=False)
    products = pd.read_csv(data_dir / "olist_products_dataset.csv", low_memory=False)
    return customers, orders, items, products


def _clean_customers(df: pd.DataFrame) -> pd.DataFrame:
    out = df.copy()
    for c in ["customer_id", "customer_unique_id"]:
        if c in out.columns:
            out[c] = out[c].astype(str).str.strip()
    out = out.dropna(subset=["customer_id", "customer_unique_id"], how="any")
    out = out.drop_duplicates(subset=["customer_id"], keep="first")
    return out


def _clean_orders(df: pd.DataFrame) -> pd.DataFrame:
    out = df.copy()
    out["order_id"] = out["order_id"].astype(str).str.strip()
    out["customer_id"] = out["customer_id"].astype(str).str.strip()
    out["order_status"] = out["order_status"].astype(str).str.strip().str.lower()
    ts = pd.to_datetime(out["order_purchase_timestamp"], errors="coerce", utc=True)
    out["order_purchase_timestamp"] = ts
    out = out.dropna(subset=["order_id", "customer_id", "order_purchase_timestamp"])
    # Model purchase signal: delivered only (avoids cancelled / unpaid noise)
    out = out[out["order_status"] == "delivered"]
    return out


def _clean_order_items(df: pd.DataFrame) -> pd.DataFrame:
    out = df.copy()
    out["order_id"] = out["order_id"].astype(str).str.strip()
    out["product_id"] = out["product_id"].astype(str).str.strip()
    out["price"] = pd.to_numeric(out["price"], errors="coerce").fillna(0.0).clip(lower=0)
    out = out.dropna(subset=["order_id", "product_id"])
    out = out.drop_duplicates(subset=["order_id", "order_item_id"] if "order_item_id" in out.columns else ["order_id", "product_id"])
    return out


def _clean_products(df: pd.DataFrame) -> pd.DataFrame:
    out = df.copy()
    out["product_id"] = out["product_id"].astype(str).str.strip()
    if "product_category_name" in out.columns:
        out["product_category_name"] = (
            out["product_category_name"].fillna("").astype(str).str.strip().replace("", "unknown")
        )
    else:
        out["product_category_name"] = "unknown"
    out = out.dropna(subset=["product_id"])
    out = out.drop_duplicates(subset=["product_id"], keep="first")
    return out


def build_line_items(customers: pd.DataFrame, orders: pd.DataFrame, items: pd.DataFrame) -> pd.DataFrame:
    """One row per line item with customer_unique_id and purchase timestamp."""
    o = orders.merge(customers, on="customer_id", how="inner")
    m = items.merge(
        o[
            [
                "order_id",
                "customer_unique_id",
                "order_purchase_timestamp",
                "order_status",
            ]
        ],
        on="order_id",
        how="inner",
    )
    m["customer_unique_id"] = m["customer_unique_id"].astype(str)
    m["product_id"] = m["product_id"].astype(str)
    return m


def clean_olist(data_dir: Path) -> CleanedOlist:
    customers, orders, items, products = load_raw_csvs(data_dir)

    profile: dict[str, Any] = {
        "raw_rows": {
            "customers": len(customers),
            "orders": len(orders),
            "order_items": len(items),
            "products": len(products),
        }
    }

    customers = _clean_customers(customers)
    orders = _clean_orders(orders)
    items = _clean_order_items(items)
    products = _clean_products(products)

    line_items = build_line_items(customers, orders, items)
    # Drop line items whose product_id is unknown in catalog
    valid_pids = set(products["product_id"].astype(str))
    before = len(line_items)
    line_items = line_items[line_items["product_id"].isin(valid_pids)]
    profile["dropped_line_items_unknown_product"] = before - len(line_items)

    profile["clean_rows"] = {
        "customers": len(customers),
        "orders": len(orders),
        "order_items": len(items),
        "products": len(products),
        "line_items": len(line_items),
    }
    if len(line_items) > 0:
        profile["order_purchase_timestamp"] = {
            "min": str(line_items["order_purchase_timestamp"].min()),
            "max": str(line_items["order_purchase_timestamp"].max()),
        }

    return CleanedOlist(line_items=line_items, products=products, profile=profile)


def line_items_to_interaction_counts(line_items: pd.DataFrame) -> pd.DataFrame:
    """Aggregate duplicate (user, product) with purchase counts."""
    g = (
        line_items.groupby(["customer_unique_id", "product_id"], as_index=False)
        .agg(purchase_count=("order_id", "count"))
        .query("purchase_count >= 1")
    )
    return g


def write_data_profile(profile: dict[str, Any], path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(profile, indent=2), encoding="utf-8")

