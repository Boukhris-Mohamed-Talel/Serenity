from __future__ import annotations

from dataclasses import dataclass

import numpy as np
from scipy.sparse import csr_matrix
from sklearn.decomposition import TruncatedSVD


@dataclass
class StoredPurchaseModel:
    """SVD factors + user row matrix slice for served users."""

    svd: TruncatedSVD
    X_users: csr_matrix  # users × products sparse (training subset)
    user_ids: list[str]
    product_ids: list[str]
    uid_index: dict[str, int]
    col_index_to_pid: dict[int, str]
    popular_product_ids: list[str]
    product_titles: dict[str, str]
    product_categories: dict[str, str]

    def recommend(self, customer_unique_id: str, top_k: int, mask_purchased: bool) -> list[tuple[str, float]]:
        top_k = max(1, min(100, top_k))
        n_products = len(self.product_ids)
        uid = self.uid_index.get(customer_unique_id)
        if uid is None:
            return [(pid, 1.0 / (i + 1)) for i, pid in enumerate(self.popular_product_ids[:top_k])]

        u_row = self.X_users.getrow(uid).astype(np.float32)
        if u_row.nnz == 0:
            return [(pid, 1.0 / (i + 1)) for i, pid in enumerate(self.popular_product_ids[:top_k])]

        scores = self.svd.inverse_transform(self.svd.transform(u_row)).ravel()
        if mask_purchased:
            purchased_cols = u_row.indices
            scores[purchased_cols] = -np.inf

        candidate_count = min(top_k + 50, n_products)
        idx = np.argpartition(-scores, candidate_count - 1)[:candidate_count]
        idx = idx[np.argsort(-scores[idx])]
        out: list[tuple[str, float]] = []
        for j in idx:
            if len(out) >= top_k:
                break
            if not np.isfinite(scores[j]) or scores[j] == -np.inf:
                continue
            pid = self.col_index_to_pid.get(int(j))
            if pid is None:
                continue
            out.append((pid, float(scores[j])))
        if len(out) < top_k:
            for pid in self.popular_product_ids:
                if len(out) >= top_k:
                    break
                if any(p == pid for p, _ in out):
                    continue
                out.append((pid, 0.0))
        return out[:top_k]
