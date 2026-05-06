from __future__ import annotations

import os
from pathlib import Path


def resolve_olist_dir(cli_dir: str | None) -> Path:
    if cli_dir:
        return Path(cli_dir).expanduser().resolve()
    env = os.environ.get("OLIST_DATA_DIR")
    if env:
        return Path(env).expanduser().resolve()
    here = Path(__file__).resolve().parent.parent
    default = here / "data" / "olist"
    if default.exists():
        return default
    return default


def try_download_kaggle_olist() -> Path | None:
    """If kagglehub is installed and user is logged in, download Olist CSVs into data/olist."""
    try:
        import kagglehub  # type: ignore
    except ImportError:
        return None
    try:
        root = Path(kagglehub.dataset_download("olistbr/brazilian-ecommerce"))
        return root
    except Exception:
        return None
