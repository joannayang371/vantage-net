"""Capacity math.

The rule itself — total - allocated - maintenance_buffer, because a link needed
during a maintenance window is not spare capacity — lives in
``unified-inventory-rules`` and is shared with the other platforms that quote
against the same inventory. This module only adapts it to site documents.
"""

from __future__ import annotations

from typing import Any, Dict

from unified_inventory_rules import AVAILABILITY_RULE, available_capacity, utilization_pct

__all__ = ["AVAILABILITY_RULE", "available_capacity", "utilization_pct", "site_capacity"]


def site_capacity(site: Dict[str, Any]) -> Dict[str, Any]:
    total = int(site.get("total_capacity_mbps", 0))
    allocated = int(site.get("allocated_mbps", 0))
    buffer_mbps = int(site.get("maintenance_buffer_mbps", 0))
    return {
        "total_mbps": total,
        "allocated_mbps": allocated,
        "maintenance_buffer_mbps": buffer_mbps,
        "available_mbps": available_capacity(total, allocated, buffer_mbps),
        "utilization_pct": utilization_pct(total, allocated, buffer_mbps),
    }
