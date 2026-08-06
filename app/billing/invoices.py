from __future__ import annotations

from decimal import Decimal
from typing import Any, Dict, List, Optional

from app.billing.discounts import apply_loyalty, apply_tax
from app.billing.rating import overage_mb, rate_overage
from app.db import all_documents


def accounts() -> List[Dict[str, Any]]:
    return all_documents("accounts")


def usage_records(account_id: Optional[str] = None, period: Optional[str] = None) -> List[Dict[str, Any]]:
    records = all_documents("usage")
    if account_id:
        records = [r for r in records if r.get("account_id") == account_id]
    if period:
        records = [r for r in records if r.get("period") == period]
    return records


def find_account(account_id: str) -> Optional[Dict[str, Any]]:
    for account in accounts():
        if account["account_id"] == account_id:
            return account
    return None


def build_invoice(account: Dict[str, Any], usage: Dict[str, Any]) -> Dict[str, Any]:
    usage_mb = int(usage["usage_mb"])
    included_gb = int(account["included_gb"])
    charges = rate_overage(usage_mb, included_gb)
    taxed = apply_tax(charges, account["tax_pct"])
    total = apply_loyalty(taxed, account["loyalty_discount_pct"])
    return {
        "account_id": account["account_id"],
        "legal_name": account["legal_name"],
        "tax_id": account["tax_id"],
        "service_address": account["service_address"],
        "period": usage["period"],
        "usage_mb": usage_mb,
        "included_gb": included_gb,
        "overage_mb": overage_mb(usage_mb, included_gb),
        "overage_charges": float(charges),
        "tax_pct": account["tax_pct"],
        "after_tax": float(taxed),
        "loyalty_discount_pct": account["loyalty_discount_pct"],
        "invoice_total": float(total),
    }


def list_invoices(account_id: Optional[str] = None, period: Optional[str] = None) -> List[Dict[str, Any]]:
    invoices = []
    for usage in usage_records(account_id=account_id, period=period):
        if not usage.get("account_id"):
            # Usage with no billing account never reaches an invoice.
            continue
        account = find_account(usage["account_id"])
        if account is None:
            continue
        invoices.append(build_invoice(account, usage))
    return invoices


def unlinked_usage(period: Optional[str] = None) -> List[Dict[str, Any]]:
    """Mediated usage carrying no billing account. Never invoiced today."""
    records = [r for r in all_documents("usage") if not r.get("account_id")]
    if period:
        records = [r for r in records if r.get("period") == period]
    return records


def billed_usage_mb(period: Optional[str] = None) -> int:
    return sum(int(i["usage_mb"]) for i in list_invoices(period=period))


def mediated_usage_mb(period: Optional[str] = None) -> int:
    records = all_documents("usage")
    if period:
        records = [r for r in records if r.get("period") == period]
    return sum(int(r["usage_mb"]) for r in records)


def revenue_total(period: Optional[str] = None) -> Decimal:
    return sum((Decimal(str(i["invoice_total"])) for i in list_invoices(period=period)), Decimal("0"))
