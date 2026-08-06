from app.inventory.circuits import active_capacity_mbps, active_count, is_active
from app.inventory.repository import list_circuits


def test_standby_circuits_are_not_active():
    assert not is_active({"lifecycle_state": "ACTIVE", "role": "STANDBY"})
    assert not is_active({"lifecycle_state": "ACTIVE", "role": "FAILOVER"})
    assert is_active({"lifecycle_state": "ACTIVE", "role": "PRIMARY"})


def test_retired_primary_is_not_active():
    assert not is_active({"lifecycle_state": "RETIRED", "role": "PRIMARY"})


def test_active_rollups_exclude_redundant_circuits():
    circuits = list_circuits()
    assert active_count(circuits) < len(circuits)
    assert active_capacity_mbps(circuits) == sum(
        c["capacity_mbps"] for c in circuits if is_active(c)
    )
