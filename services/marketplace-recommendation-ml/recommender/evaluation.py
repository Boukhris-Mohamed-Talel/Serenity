"""Temporal hold-out evaluation for implicit matrix factorization."""

from __future__ import annotations

from dataclasses import dataclass

import numpy as np
import pandas as pd
from scipy.sparse import csr_matrix
from sklearn.decomposition import TruncatedSVD


@dataclass
class EvalMetrics:
    hit_rate_at_k: float
    mean_rank_percentile: float
    n_evaluated: int
    n_skipped_cold_user: int
    test_edges: int


def temporal_train_mask(line_items: pd.DataFrame, test_fraction: float = 0.2) -> pd.Series:
    """True = train row, False = test row. Rows with newest ~test_fraction of timestamps go to test."""
    ts = pd.to_datetime(line_items["order_purchase_timestamp"], errors="coerce", utc=True)
    if ts.notna().sum() < 10:
        return pd.Series(True, index=line_items.index)
    cutoff = ts.quantile(1.0 - test_fraction)
    return ts <= cutoff


def build_sparse_matrix(
    interactions: pd.DataFrame,
    user_ids: list[str],
    product_ids: list[str],
) -> csr_matrix:
    uid_index = {u: i for i, u in enumerate(user_ids)}
    pid_index = {p: j for j, p in enumerate(product_ids)}
    rows = interactions["customer_unique_id"].map(uid_index).astype(np.int32).values
    cols = interactions["product_id"].map(pid_index).astype(np.int32).values
    data = interactions["purchase_count"].astype(np.float32).values
    return csr_matrix((data, (rows, cols)), shape=(len(user_ids), len(product_ids)))


def evaluate_hit_rate(
    svd: TruncatedSVD,
    X_train: csr_matrix,
    test_pairs: pd.DataFrame,
    uid_index: dict[str, int],
    pid_index: dict[str, int],
    k: int = 10,
    max_users: int = 8000,
    random_state: int = 42,
) -> EvalMetrics:
    """
    For each test (user, product), use only X_train row for the user.
    Hit@K: true product in top-K by predicted score (excluding -inf masked train items optional — we rank all).
    """
    rng = np.random.default_rng(random_state)
    if len(test_pairs) == 0:
        return EvalMetrics(0.0, 1.0, 0, 0, 0)

    # subsample users for speed
    users = test_pairs["customer_unique_id"].unique().tolist()
    if len(users) > max_users:
        users = list(rng.choice(np.array(users), size=max_users, replace=False))
        test_pairs = test_pairs[test_pairs["customer_unique_id"].isin(users)]

    hits = 0
    rank_pct_sum = 0.0
    n_ok = 0
    n_skip = 0
    n_products = X_train.shape[1]

    for uid, sub in test_pairs.groupby("customer_unique_id"):
        uix = uid_index.get(str(uid))
        if uix is None:
            n_skip += 1
            continue
        row = X_train.getrow(uix)
        if row.nnz == 0:
            n_skip += 1
            continue
        scores = svd.inverse_transform(svd.transform(row)).ravel()
        # rank all products by score descending
        order = np.argsort(-scores)

        for _, r in sub.iterrows():
            pid = str(r["product_id"])
            pix = pid_index.get(pid)
            if pix is None:
                continue
            rank = int(np.where(order == pix)[0][0]) if pix in order else n_products
            rank_pct_sum += rank / max(1, n_products - 1)
            if rank < k:
                hits += 1
            n_ok += 1

    hit_rate = hits / n_ok if n_ok else 0.0
    mean_rank_pct = rank_pct_sum / n_ok if n_ok else 1.0
    return EvalMetrics(
        hit_rate_at_k=hit_rate,
        mean_rank_percentile=mean_rank_pct,
        n_evaluated=n_ok,
        n_skipped_cold_user=n_skip,
        test_edges=len(test_pairs),
    )
