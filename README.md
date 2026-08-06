# vantage-net

Inventory, management addressing and billing services for the Vantage network.

Vantage runs a single FastAPI application backed by MongoDB. For local
development and CI the same collections are read from `data/seed`, so the app
and the test suite run with no database.

## Getting started

```bash
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

Point the app at a real database by exporting `MONGO_URI` (and optionally
`MONGO_DB`, default `vantage`).

## Modules

### `app/inventory`

Network resources keyed by `device_uuid`, grouped by `market_id` (fine-grained
metro codes), with a five-state `lifecycle_state`: `ACTIVE`, `MAINTENANCE`,
`RETIRED`, `PLANNED`, `RESERVED`.

- `capacity.py` — `available = total - allocated - maintenance_buffer`. A link
  reserved for maintenance is not spare capacity.
- `circuits.py` — standby and failover circuits are excluded from active counts
  and active capacity.

Endpoints: `GET /resources`, `GET /resources/{device_uuid}`, `GET /circuits`.

### `app/network`

Management addressing. Every device holds an address out of the
`10.20.0.0/16` management supernet; `10.20.250.0/24` is the growth pool.
Generated artifacts under `app/network/configs` reference those addresses:

| Path | Format | Purpose |
| --- | --- | --- |
| `configs/routing/*.yaml` | YAML | per-market static routes |
| `configs/firewall/*.yaml` | YAML | management-plane ACLs |
| `configs/dns/*.zone` | zone file | forward records for management names |
| `configs/monitoring/*.json` | JSON | NOC poller targets |
| `configs/address_plan.json` | JSON | supernet, growth pool, external-BGP devices |

`addressing.py` can list every config file referencing a given address and
detect references to addresses no device owns.

Endpoints: `GET /network/addressing`, `GET /network/devices`,
`GET /network/references?address=...`.

Devices flagged `external_bgp` are customer-facing and must not be renumbered.

### `app/billing`

- `rating.py` — usage is billed to the exact MB at $0.012/MB overage. No
  gigabyte rounding.
- `discounts.py` — tax is assessed on the full charge; the loyalty discount is
  applied to the taxed total afterwards.
- `invoices.py` — invoice construction, plus `unlinked_usage()` for usage that
  was mediated without a billing account.

Endpoints: `GET /billing/invoices`, `GET /billing/usage-summary`.

## Capacity check (sales engineering)

`GET /capacity` is the sales-facing page: enter the bandwidth being quoted and
it shows, per customer location, what is actually sellable. Availability comes
from `app/inventory/capacity.py`, so the maintenance buffer is withheld:

```
available = total_capacity - allocated - maintenance_buffer
```

Serviceable locations live in `data/seed/locations.json`. The same figures are
available as JSON from `GET /capacity/locations?requested_mbps=...` and
`GET /capacity/locations/{location_code}`.

## Dashboard

`GET /dashboard` renders the current inventory and billing figures as a plain
HTML page.

## Tests

```bash
pytest
```

`tests/` covers capacity, circuit roll-ups, rating and discount ordering,
addressing/reference integrity, and the HTTP surface.

## Seed data

`data/seed/` holds `sites.json` (97), `circuits.json`, `devices.json`,
`accounts.json`, `usage.json` and `locations.json`.
