from app.inventory.capacity import available_capacity, site_capacity, utilization_pct
from app.inventory.repository import get_site, list_circuits


def test_available_capacity_subtracts_maintenance_buffer():
    assert available_capacity(1000, 600, 150) == 250


def test_available_capacity_never_negative():
    assert available_capacity(100, 90, 50) == 0


def test_utilization_includes_buffer():
    assert utilization_pct(1000, 600, 150) == 75.0


def test_downtown_fiber_ring_reports_250_mbps():
    circuit = list_circuits(circuit_id="VC-BOS-0118")[0]
    assert circuit["available_mbps"] == 250


def test_site_capacity_shape():
    site = get_site(list_sites_uuid())
    capacity = site_capacity(site)
    assert set(capacity) == {
        "total_mbps",
        "allocated_mbps",
        "maintenance_buffer_mbps",
        "available_mbps",
        "utilization_pct",
    }


def list_sites_uuid():
    from app.inventory.repository import list_sites

    return list_sites()[0]["device_uuid"]
