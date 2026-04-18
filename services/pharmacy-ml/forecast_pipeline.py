import math
import os
from datetime import date, datetime, timedelta
from typing import Optional

import pandas as pd
from sqlalchemy import create_engine, text
from statsforecast import StatsForecast
from statsforecast.models import TSB


DB_URL = os.getenv("PHARMACY_ML_DB_URL", "mysql+pymysql://root:@localhost:3306/pharmacy_db")
LOOKBACK_DAYS = int(os.getenv("PHARMACY_ML_LOOKBACK_DAYS", "180"))
HORIZON_DAYS = int(os.getenv("PHARMACY_ML_HORIZON_DAYS", "14"))
SHORT_HISTORY_DAYS = int(os.getenv("PHARMACY_ML_SHORT_HISTORY_DAYS", "21"))
TSB_ALPHA_D = float(os.getenv("PHARMACY_ML_TSB_ALPHA_D", "0.3"))
TSB_ALPHA_P = float(os.getenv("PHARMACY_ML_TSB_ALPHA_P", "0.3"))

engine = create_engine(DB_URL, future=True)


def _load_events(pharmacy_id: Optional[int]) -> pd.DataFrame:
    sql = """
        select pharmacy_id, lower(medicine_name) as medicine_name, date(event_at) as ds, sum(consumed_qty) as y
        from stock_consumption_events
        where event_at >= :cutoff
    """
    params = {"cutoff": datetime.now() - timedelta(days=LOOKBACK_DAYS)}
    if pharmacy_id is not None:
        sql += " and pharmacy_id = :pharmacy_id"
        params["pharmacy_id"] = pharmacy_id
    sql += " group by pharmacy_id, lower(medicine_name), date(event_at) order by pharmacy_id, medicine_name, ds"

    with engine.connect() as conn:
        rows = conn.execute(text(sql), params).fetchall()

    if not rows:
        return pd.DataFrame(columns=["pharmacy_id", "medicine_name", "ds", "y"])
    df = pd.DataFrame(rows, columns=["pharmacy_id", "medicine_name", "ds", "y"])
    df["ds"] = pd.to_datetime(df["ds"])
    df["y"] = pd.to_numeric(df["y"], errors="coerce").fillna(0.0)
    return df


def _build_series(events_df: pd.DataFrame) -> tuple[pd.DataFrame, pd.DataFrame]:
    if events_df.empty:
        return (
            pd.DataFrame(columns=["unique_id", "ds", "y"]),
            pd.DataFrame(columns=["unique_id", "pharmacy_id", "medicine_name", "medicine_key", "history_days"]),
        )

    today = pd.Timestamp(date.today())
    train_parts: list[pd.DataFrame] = []
    meta_rows: list[dict] = []

    for (pharmacy_id, medicine_name), group in events_df.groupby(["pharmacy_id", "medicine_name"]):
        medicine_key = str(medicine_name).strip().lower()
        unique_id = f"{int(pharmacy_id)}::{medicine_key}"
        daily = group.groupby("ds", as_index=False)["y"].sum()
        full_days = pd.DataFrame({"ds": pd.date_range(start=daily["ds"].min(), end=today, freq="D")})
        full_series = full_days.merge(daily, on="ds", how="left")
        full_series["y"] = full_series["y"].fillna(0.0).astype(float)
        full_series["unique_id"] = unique_id

        train_parts.append(full_series[["unique_id", "ds", "y"]])
        meta_rows.append(
            {
                "unique_id": unique_id,
                "pharmacy_id": int(pharmacy_id),
                "medicine_name": str(medicine_name),
                "medicine_key": medicine_key,
                "history_days": int(len(full_series)),
            }
        )

    return pd.concat(train_parts, ignore_index=True), pd.DataFrame(meta_rows)


def _normalize_sf_output(df: pd.DataFrame) -> pd.DataFrame:
    out = df if "unique_id" in df.columns else df.reset_index()
    if "unique_id" not in out.columns:
        out = out.rename(columns={out.columns[0]: "unique_id"})
    return out


def _forecast(train_df: pd.DataFrame, meta_df: pd.DataFrame) -> pd.DataFrame:
    if train_df.empty or meta_df.empty:
        return pd.DataFrame(columns=["unique_id", "forecast_date", "predicted_demand", "model_type"])

    long_ids = meta_df[meta_df["history_days"] >= SHORT_HISTORY_DAYS]["unique_id"].tolist()
    short_ids = meta_df[meta_df["history_days"] < SHORT_HISTORY_DAYS]["unique_id"].tolist()
    output_frames: list[pd.DataFrame] = []

    if long_ids:
        sf = StatsForecast(models=[TSB(alpha_d=TSB_ALPHA_D, alpha_p=TSB_ALPHA_P)], freq="D", n_jobs=1)
        tsb = sf.forecast(df=train_df[train_df["unique_id"].isin(long_ids)], h=HORIZON_DAYS)
        tsb = _normalize_sf_output(tsb).rename(columns={"ds": "forecast_date", "TSB": "predicted_demand"})
        tsb["model_type"] = "TSB"
        output_frames.append(tsb[["unique_id", "forecast_date", "predicted_demand", "model_type"]])

    if short_ids:
        dates = pd.date_range(start=date.today() + timedelta(days=1), periods=HORIZON_DAYS, freq="D")
        rows: list[dict] = []
        for unique_id in short_ids:
            mean_7 = float(train_df[train_df["unique_id"] == unique_id]["y"].tail(7).mean())
            if pd.isna(mean_7):
                mean_7 = 0.0
            mean_7 = max(0.0, mean_7)
            for forecast_date in dates:
                rows.append(
                    {
                        "unique_id": unique_id,
                        "forecast_date": forecast_date,
                        "predicted_demand": mean_7,
                        "model_type": "FALLBACK_MEAN",
                    }
                )
        output_frames.append(pd.DataFrame(rows))

    if not output_frames:
        return pd.DataFrame(columns=["unique_id", "forecast_date", "predicted_demand", "model_type"])
    out = pd.concat(output_frames, ignore_index=True)
    out["predicted_demand"] = out["predicted_demand"].clip(lower=0.0)
    return out


def _load_stock_map(pharmacy_id: Optional[int]) -> dict[tuple[int, str], int]:
    sql = """
        select pharmacy_id, lower(medicine_name) as medicine_key, sum(coalesce(quantity, 0)) as current_qty
        from medicine_stock_items
        where archived = false
    """
    params: dict = {}
    if pharmacy_id is not None:
        sql += " and pharmacy_id = :pharmacy_id"
        params["pharmacy_id"] = pharmacy_id
    sql += " group by pharmacy_id, lower(medicine_name)"

    with engine.connect() as conn:
        rows = conn.execute(text(sql), params).fetchall()
    return {(int(row[0]), str(row[1])): int(row[2] or 0) for row in rows}


def _risk(current_qty: int, demand_14: float) -> str:
    if demand_14 <= 0:
        return "LOW"
    ratio = current_qty / demand_14
    if ratio < 0.5:
        return "HIGH"
    if ratio < 1.0:
        return "MEDIUM"
    return "LOW"


def _persist(result_df: pd.DataFrame) -> int:
    if result_df.empty:
        return 0

    rows = result_df.to_dict(orient="records")
    run_at = datetime.now()
    pharmacy_ids = sorted({int(row["pharmacy_id"]) for row in rows})

    delete_sql = text("delete from stock_forecast_predictions where pharmacy_id = :pharmacy_id")
    insert_sql = text(
        """
        insert into stock_forecast_predictions (
            run_at, model_type, pharmacy_id, medicine_name, forecast_date,
            predicted_demand, stockout_risk, suggested_reorder_qty
        ) values (
            :run_at, :model_type, :pharmacy_id, :medicine_name, :forecast_date,
            :predicted_demand, :stockout_risk, :suggested_reorder_qty
        )
        """
    )

    payload = [
        {
            "run_at": run_at,
            "model_type": row["model_type"],
            "pharmacy_id": int(row["pharmacy_id"]),
            "medicine_name": row["medicine_name"],
            "forecast_date": pd.to_datetime(row["forecast_date"]).date(),
            "predicted_demand": float(row["predicted_demand"]),
            "stockout_risk": row["stockout_risk"],
            "suggested_reorder_qty": int(row["suggested_reorder_qty"]),
        }
        for row in rows
    ]

    with engine.begin() as conn:
        for pharmacy_id in pharmacy_ids:
            conn.execute(delete_sql, {"pharmacy_id": pharmacy_id})
        conn.execute(insert_sql, payload)
    return len(payload)


def run_forecast_pipeline(pharmacy_id: Optional[int]) -> dict:
    # 1) Read historical demand from DB.
    events_df = _load_events(pharmacy_id)
    if events_df.empty:
        if pharmacy_id is not None:
            with engine.begin() as conn:
                conn.execute(
                    text("delete from stock_forecast_predictions where pharmacy_id = :pharmacy_id"),
                    {"pharmacy_id": pharmacy_id},
                )
        return {"rowsInserted": 0, "pharmacyCount": 0, "medicineCount": 0}

    # 2) Build daily series per pharmacy+medicine.
    train_df, meta_df = _build_series(events_df)

    # 3) Predict 14 days with TSB (or fallback mean for short history).
    forecast_df = _forecast(train_df, meta_df)
    if forecast_df.empty:
        return {"rowsInserted": 0, "pharmacyCount": 0, "medicineCount": 0}

    # 4) Compute reorder qty + risk from forecast and current stock.
    stock_map = _load_stock_map(pharmacy_id)
    merged = forecast_df.merge(meta_df, on="unique_id", how="left")

    final_rows: list[dict] = []
    for _, group in merged.groupby("unique_id"):
        first = group.iloc[0]
        pharmacy_value = int(first["pharmacy_id"])
        medicine_name = str(first["medicine_name"])
        medicine_key = str(first["medicine_key"])
        current_qty = stock_map.get((pharmacy_value, medicine_key), 0)

        demand_14 = float(group["predicted_demand"].sum())
        safety_stock = int(math.ceil(float(group["predicted_demand"].head(7).mean()) * 2.0))
        suggested_reorder_qty = max(0, int(math.ceil(demand_14 + safety_stock - current_qty)))
        stockout_risk = _risk(current_qty, demand_14)

        for _, row in group.iterrows():
            final_rows.append(
                {
                    "pharmacy_id": pharmacy_value,
                    "medicine_name": medicine_name,
                    "forecast_date": row["forecast_date"],
                    "predicted_demand": float(row["predicted_demand"]),
                    "model_type": row["model_type"],
                    "suggested_reorder_qty": suggested_reorder_qty,
                    "stockout_risk": stockout_risk,
                }
            )

    result_df = pd.DataFrame(final_rows)
    rows_inserted = _persist(result_df)
    return {
        "rowsInserted": rows_inserted,
        "pharmacyCount": int(result_df["pharmacy_id"].nunique()),
        "medicineCount": int(result_df[["pharmacy_id", "medicine_name"]].drop_duplicates().shape[0]),
    }
