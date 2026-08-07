"""Capacity math.

The arithmetic itself no longer lives here. It is owned by the canonical rules
repository and shared with meridian-oss, so the two systems cannot drift apart:

    https://github.com/joannayang371/unified-inventory-rules

Vantage always holds back a maintenance buffer on top of what is allocated: a
link is not considered spare capacity if we need it during a maintenance
window. ``available`` is therefore total - allocated - maintenance_buffer, and
utilization counts the buffer as used. This module stays as the import path the
rest of the app already uses; behaviour is unchanged.
"""

from __future__ import annotations

from oss_capacity import available_capacity, site_capacity, utilization_pct

__all__ = ["available_capacity", "utilization_pct", "site_capacity"]
