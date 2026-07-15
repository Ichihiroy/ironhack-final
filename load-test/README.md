# Load test — HPA demo + honest DB-backed p95

One k6 script, two concurrent scenarios, scoring **Scalability** and
**Observability** at the same time:

| Scenario   | Endpoint | Path exercised | Peak rate | Threshold |
| ---------- | -------- | -------------- | --------- | --------- |
| `findings` | `GET /api/scans/demo/findings` | **Redis cache** (hot read) | 350 req/s | p95 ≤ 300 ms |
| `db_reads` | `GET /api/scans/demo/optimized` | **Database** — scan lookup + findings query per request, deliberately uncached | 30 req/s | p95 ≤ 500 ms |

The cached scenario is the HPA hero: it pushes backend CPU over the 70% target
so replicas visibly climb 3 → max on the Grafana Platform dashboard. The DB
scenario is what makes the p95 number *mean* something — it round-trips Azure
SQL through the private endpoint on every request, so connection-pool or
DB-side saturation shows up in its latency (watch the *DB connection pool*
panel on the Backend API dashboard) instead of being hidden by Redis.

The `demo` scan is seeded on startup (`DemoDataSeeder`, ~$2,300/mo of flagged
waste). Zero setup: no upload needed before running.

## Run it

```bash
# against the public ingress (production-shaped demo)
k6 run -e BASE_URL=https://<your-host> items-load.js

# or against a port-forward (no ingress/DNS needed)
kubectl -n app-staging port-forward svc/backend 8080:8080 &
k6 run -e BASE_URL=http://localhost:8080 items-load.js

# locally against docker-compose
docker compose up --build -d
k6 run -e BASE_URL=http://localhost:8080 items-load.js
```

Both scenarios run the same profile: 1m warm-up → 2m ramp → **5m sustain** →
1m cool-down (~9 minutes total). Thresholds are per-scenario (k6 tags each
request with its scenario) and fail the run (non-zero exit) if either p95
budget is blown or the overall error rate is ≥ 1%. Override the target scan
with `-e SCAN_ID=<id>`.

## What to watch while it runs

1. Grafana → **Platform** dashboard → *HPA — current vs desired vs max*:
   desired replicas step from 3 upward as CPU crosses the target; current
   follows as the cluster autoscaler adds user-pool nodes if needed.
2. Grafana → **Backend API** dashboard: request rate climbs to ~380 req/s
   total; *Latency p95 by endpoint* separates the cached findings read from
   the DB-backed optimized read; *DB connection pool* shows `active`
   approaching `max` if the database becomes the bottleneck.
3. Grafana → **Edge** dashboard: the same latency measured at the NGINX
   ingress, per service.
4. Terminal:

   ```bash
   kubectl -n app-staging get hpa backend --watch
   ```

After cool-down, replicas return to 3 following the HPA stabilization window
(~5 minutes) — worth showing in the demo too.
