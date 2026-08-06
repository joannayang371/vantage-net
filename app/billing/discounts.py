"""Discount and tax application.

Tax is assessed on the full pre-discount charge; the loyalty discount is a
goodwill credit applied to the taxed total afterwards.
"""

from __future__ import annotations

from decimal import Decimal

from app.billing.rating import money


def apply_tax(amount: Decimal, tax_pct: float) -> Decimal:
    return money(Decimal(amount) * (Decimal(1) + Decimal(str(tax_pct)) / Decimal(100)))


def apply_loyalty(amount: Decimal, loyalty_pct: float) -> Decimal:
    return money(Decimal(amount) * (Decimal(1) - Decimal(str(loyalty_pct)) / Decimal(100)))


def invoice_total(charges: Decimal, loyalty_pct: float, tax_pct: float) -> Decimal:
    taxed = apply_tax(charges, tax_pct)
    return apply_loyalty(taxed, loyalty_pct)
