"""Usage rating.

Vantage rates to the exact megabyte — customers are billed for what they used,
never for a rounded-up gigabyte — at a flat per-MB overage rate.
"""

from __future__ import annotations

from decimal import ROUND_HALF_UP, Decimal

OVERAGE_RATE_PER_MB = Decimal("0.012")
MB_PER_GB = 1024


def overage_mb(usage_mb: int, included_gb: int) -> int:
    included_mb = included_gb * MB_PER_GB
    return max(usage_mb - included_mb, 0)


def rate_overage(usage_mb: int, included_gb: int) -> Decimal:
    charge = Decimal(overage_mb(usage_mb, included_gb)) * OVERAGE_RATE_PER_MB
    return money(charge)


def money(amount: Decimal) -> Decimal:
    return Decimal(amount).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
