"""Data access.

Production runs against MongoDB (``MONGO_URI``). For local development and the
test suite the same collections are served from the JSON documents under
``data/seed``, so nothing here needs a running database.
"""

from __future__ import annotations

import json
import os
from functools import lru_cache
from pathlib import Path
from typing import Any, Dict, Iterable, List, Optional

SEED_DIR = Path(__file__).resolve().parent.parent / "data" / "seed"

COLLECTIONS = {
    "sites": "sites.json",
    "circuits": "circuits.json",
    "accounts": "accounts.json",
    "usage": "usage.json",
    "devices": "devices.json",
}


class SeedCollection:
    """Read-only stand-in for a Mongo collection backed by a seed file."""

    def __init__(self, documents: List[Dict[str, Any]]) -> None:
        self._documents = documents

    def find(self, query: Optional[Dict[str, Any]] = None) -> Iterable[Dict[str, Any]]:
        for doc in self._documents:
            if _matches(doc, query or {}):
                yield dict(doc)

    def find_one(self, query: Optional[Dict[str, Any]] = None) -> Optional[Dict[str, Any]]:
        for doc in self.find(query):
            return doc
        return None

    def count_documents(self, query: Optional[Dict[str, Any]] = None) -> int:
        return sum(1 for _ in self.find(query))


def _matches(doc: Dict[str, Any], query: Dict[str, Any]) -> bool:
    return all(doc.get(key) == value for key, value in query.items())


@lru_cache(maxsize=None)
def _load_seed(name: str) -> SeedCollection:
    with (SEED_DIR / COLLECTIONS[name]).open() as handle:
        return SeedCollection(json.load(handle))


def get_collection(name: str):
    """Return the named collection, from Mongo when configured, else the seed."""
    if name not in COLLECTIONS:
        raise KeyError(f"unknown collection: {name}")
    uri = os.environ.get("MONGO_URI")
    if not uri:
        return _load_seed(name)
    from pymongo import MongoClient  # imported lazily: unused in seed mode

    client = MongoClient(uri)
    return client[os.environ.get("MONGO_DB", "vantage")][name]


def all_documents(name: str) -> List[Dict[str, Any]]:
    return list(get_collection(name).find({}))
