# Load test — the HPA hero demo

One k6 script that scores **Scalability** and **Observability** at the same
time: it drives `GET /api/items` past **300 req/s** (sustained), asserts
**p95 ≤ 300 ms**, and pushes backend CPU over the HPA's 70% target so replicas
visibly climb on the Grafana dashboard.

## Run it

```bash
# against the public ingress (production-shaped demo)
k6 run -e BASE_URL=https://<your-host> items-load.js

# or against a port-forward (no ingress/DNS needed)
kubectl -n app-staging port-forward svc/backend 8080:8080 &
k6 run -e BASE_URL=http://localhost:8080 items-load.js
```

Profile: 1m warm-up → 2m ramp to 350 req/s → **5m sustain** → 1m cool-down
(~9 minutes total). Thresholds fail the run (non-zero exit) if p95 > 300 ms or
error rate ≥ 1%.

## What to watch while it runs

1. Grafana → **Platform** dashboard → *HPA — current vs desired vs max*:
   desired replicas step from 3 upward as CPU crosses the target; current
   follows as the cluster autoscaler adds user-pool nodes if needed.
2. Grafana → **Backend API** dashboard: request rate climbs to ~350 req/s while
   p95 stays under 300 ms — that's the scaling working.
3. Terminal:

   ```bash
   kubectl -n app-staging get hpa backend --watch
   ```

After cool-down, replicas return to 3 following the HPA stabilization window
(~5 minutes) — worth showing in the demo too.
