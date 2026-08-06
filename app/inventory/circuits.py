"""Circuit roll-ups.

Standby and failover circuits carry no customer traffic, so they are excluded
from the active count and from active capacity. Counting them would overstate
both the footprint and the utilisation of the network.
"""

from __future__ import annotations

from typing import Any, Dict, Iterable, List

ACTIVE_ROLES = {"PRIMARY"}
REDUNDANT_ROLES = {"STANDBY", "FAILOVER"}


def is_active(circuit: Dict[str, Any]) -> bool:
    return (
        circuit.get("lifecycle_state") == "ACTIVE"
        and circuit.get("role", "PRIMARY").upper() in ACTIVE_ROLES
    )


def active_circuits(circuits: Iterable[Dict[str, Any]]) -> List[Dict[str, Any]]:
    return [c for c in circuits if is_active(c)]


def active_count(circuits: Iterable[Dict[str, Any]]) -> int:
    return len(active_circuits(circuits))


def active_capacity_mbps(circuits: Iterable[Dict[str, Any]]) -> int:
    return sum(int(c.get("capacity_mbps", 0)) for c in active_circuits(circuits))
