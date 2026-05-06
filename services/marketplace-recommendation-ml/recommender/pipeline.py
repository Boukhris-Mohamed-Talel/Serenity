"""
End-to-end: clean Olist → temporal eval → train final SVD on full data → artifacts.

Writes:
  artifacts/model.joblib
  artifacts/model_meta.json
  artifacts/metrics.json
  artifacts/data_profile.json
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path

import joblib
from sklearn.decomposition import TruncatedSVD

from recommender.cleaning import CleanedOlist, clean_olist, line_items_to_interaction_counts, write_data_profile
from recommender.evaluation import EvalMetrics, build_sparse_matrix, evaluate_hit_rate, temporal_train_mask
from recommender.io_util import resolve_olist_dir, try_download_kaggle_olist
from recommender.train_matrix import StoredPurchaseModel


def _filter_by_support(
    inter: pd.DataFrame, min_user_purchases: int, min_product_purchases: int
) -> pd.DataFrame:
    uc = inter.groupby("customer_unique_id")["purchase_count"].sum()
    pc = inter.groupby("product_id")["purchase_count"].sum()
    return inter[
        inter["customer_unique_id"].isin(uc[uc >= min_user_purchases].index)
        & inter["product_id"].isin(pc[pc >= min_product_purchases].index)
    ]


def _product_meta(products: pd.DataFrame) -> tuple[dict[str, str], dict[str, str]]:
    titles: dict[str, str] = {}
    cats: dict[str, str] = {}
    for _, r in products.iterrows():
        pid = str(r["product_id"])
        cat = str(r.get("product_category_name", "") or "")
        titles[pid] = cat
        cats[pid] = cat
    return titles, cats


def _popular(inter: pd.DataFrame, n: int = 50) -> list[str]:
    return (
        inter.groupby("product_id")["purchase_count"]
        .sum()
        .sort_values(ascending=False)
        .head(n)
        .index.astype(str)
        .tolist()
    )


def run_pipeline(
    data_dir: Path,
    artifacts_dir: Path,
    n_components: int = 32,
    min_user_purchases: int = 3,
    min_product_purchases: int = 10,
    test_fraction: float = 0.2,
    eval_k: int = 10,
    eval_max_users: int = 8000,
    skip_eval: bool = False,
) -> None:
    cleaned: CleanedOlist = clean_olist(data_dir)
    artifacts_dir.mkdir(parents=True, exist_ok=True)
    write_data_profile(cleaned.profile, artifacts_dir / "data_profile.json")

    line = cleaned.line_items
    train_mask = temporal_train_mask(line, test_fraction=test_fraction)
    train_lines = line[train_mask]
    test_lines = line[~train_mask]

    inter_full = line_items_to_interaction_counts(line)
    inter_full = _filter_by_support(inter_full, min_user_purchases, min_product_purchases)

    user_ids = sorted(inter_full["customer_unique_id"].unique().tolist())
    product_ids = sorted(inter_full["product_id"].unique().tolist())
    uid_index = {u: i for i, u in enumerate(user_ids)}
    pid_index = {p: j for j, p in enumerate(product_ids)}
    col_index_to_pid = {j: p for p, j in pid_index.items()}

    inter_train = line_items_to_interaction_counts(train_lines)
    inter_train = inter_train[
        inter_train["customer_unique_id"].isin(user_ids) & inter_train["product_id"].isin(product_ids)
    ]
    # re-aggregate counts after filter
    inter_train = (
        inter_train.groupby(["customer_unique_id", "product_id"], as_index=False)["purchase_count"]
        .sum()
    )

    X_full = build_sparse_matrix(inter_full, user_ids, product_ids)
    X_train = build_sparse_matrix(inter_train, user_ids, product_ids)

    # TruncatedSVD requires n_components < min(n_samples, n_features)
    max_allowed = max(1, min(X_full.shape[0], X_full.shape[1]) - 1)
    n_comp = max(1, min(n_components, max_allowed))

    metrics: dict = {
        "cleaning": cleaned.profile,
        "matrix": {
            "n_users": len(user_ids),
            "n_products": len(product_ids),
            "nnz_full": int(X_full.nnz),
            "nnz_train_for_eval": int(X_train.nnz),
        },
        "config": {
            "n_components": n_comp,
            "min_user_purchases": min_user_purchases,
            "min_product_purchases": min_product_purchases,
            "test_fraction": test_fraction,
            "eval_k": eval_k,
        },
    }

    eval_result: EvalMetrics | None = None
    if not skip_eval and len(test_lines) > 50:
        test_pairs = line_items_to_interaction_counts(test_lines)
        test_pairs = test_pairs[
            test_pairs["customer_unique_id"].isin(user_ids) & test_pairs["product_id"].isin(product_ids)
        ]
        svd_eval = TruncatedSVD(n_components=n_comp, random_state=42)
        svd_eval.fit(X_train)
        eval_result = evaluate_hit_rate(
            svd_eval,
            X_train,
            test_pairs,
            uid_index,
            pid_index,
            k=eval_k,
            max_users=eval_max_users,
        )
        metrics["evaluation"] = {
            "hit_rate_at_k": eval_result.hit_rate_at_k,
            "mean_rank_percentile": eval_result.mean_rank_percentile,
            "n_evaluated": eval_result.n_evaluated,
            "n_skipped_cold_user": eval_result.n_skipped_cold_user,
            "test_edges": eval_result.test_edges,
            "note": "SVD fit on train-time matrix only; final saved model is fit on full interactions.",
        }
    else:
        metrics["evaluation"] = {"skipped": True, "reason": "too_few_rows_or_skip_eval"}

    svd_final = TruncatedSVD(n_components=n_comp, random_state=42)
    svd_final.fit(X_full)

    titles, cats = _product_meta(cleaned.products)
    model = StoredPurchaseModel(
        svd=svd_final,
        X_users=X_full,
        user_ids=user_ids,
        product_ids=product_ids,
        uid_index=uid_index,
        col_index_to_pid=col_index_to_pid,
        popular_product_ids=_popular(inter_full),
        product_titles=titles,
        product_categories=cats,
    )

    model_path = artifacts_dir / "model.joblib"
    joblib.dump(model, model_path)

    meta = {
        "n_users": len(user_ids),
        "n_products": len(product_ids),
        "n_components": n_comp,
        "data_dir": str(data_dir.resolve()),
        "model_path": str(model_path.resolve()),
    }
    (artifacts_dir / "model_meta.json").write_text(json.dumps(meta, indent=2), encoding="utf-8")
    (artifacts_dir / "metrics.json").write_text(json.dumps(metrics, indent=2), encoding="utf-8")

    print(json.dumps({"artifacts": str(artifacts_dir), "meta": meta, "evaluation": metrics.get("evaluation")}, indent=2))


def main() -> None:
    p = argparse.ArgumentParser(description="Clean Olist data, evaluate (temporal hold-out), train, write artifacts.")
    p.add_argument("--data-dir", type=str, default=None)
    p.add_argument("--artifacts-dir", type=str, default="artifacts")
    p.add_argument("--components", type=int, default=32)
    p.add_argument("--min-user-purchases", type=int, default=3)
    p.add_argument("--min-product-purchases", type=int, default=10)
    p.add_argument("--test-fraction", type=float, default=0.2)
    p.add_argument("--eval-k", type=int, default=10)
    p.add_argument("--eval-max-users", type=int, default=8000)
    p.add_argument("--skip-eval", action="store_true", help="Skip temporal evaluation (faster, tiny data)")
    p.add_argument("--try-kaggle-download", action="store_true")
    args = p.parse_args()

    data_dir = Path(resolve_olist_dir(args.data_dir))
    if not (data_dir / "olist_orders_dataset.csv").exists() and args.try_kaggle_download:
        dl = try_download_kaggle_olist()
        if dl is not None:
            data_dir = Path(dl)

    run_pipeline(
        data_dir=data_dir,
        artifacts_dir=Path(args.artifacts_dir).resolve(),
        n_components=args.components,
        min_user_purchases=args.min_user_purchases,
        min_product_purchases=args.min_product_purchases,
        test_fraction=args.test_fraction,
        eval_k=args.eval_k,
        eval_max_users=args.eval_max_users,
        skip_eval=args.skip_eval,
    )


if __name__ == "__main__":
    main()
