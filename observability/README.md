# Observability

Prometheus + Grafana + Alertmanager via **kube-prometheus-stack**, scraping the
backend's Micrometer endpoint (`/actuator/prometheus`).

## Install (automatic — no manual steps)

The infra pipeline installs everything; nothing here is applied by hand (the
training subscription is wiped nightly, so hand-installed pieces wouldn't
survive — see GUIDE.md "Morning kickstart"):

- **kube-prometheus-stack** chart: installed by Terraform
  (`infra/terraform/platform.tf`) with `kube-prometheus-stack-values.yaml`
  from this directory. The Grafana admin password is Terraform-generated and
  stored only in Key Vault (`grafana-admin-password`).
- **ServiceMonitor, alert rules, dashboards**: synced by the `observability`
  Argo CD Application (`infra/terraform/gitops.tf`) via `kustomization.yaml`
  in this directory — its `configMapGenerator` wraps each `dashboards/*.json`
  into a ConfigMap labeled `grafana_dashboard=1`, which the Grafana sidecar
  auto-loads. To add a dashboard: drop the JSON in `dashboards/`, add a
  generator entry, commit.

| Dashboard              | Shows                                                        |
| ---------------------- | ------------------------------------------------------------ |
| `backend-api.json`     | Request rate, 5xx rate, p50/p95/p99 + per-endpoint p95 + latency heatmap, HikariCP pool, JVM heap, CPU/memory vs limit |
| `platform.json`        | Pod/node CPU+memory, **HPA activity**, restarts, deploy status, memory-vs-limit and CPU-throttling saturation |
| `edge-services.json`   | Per-service latency histograms and 5xx at the NGINX ingress — the only latency signal for the frontend, which has no app metrics |

Latency panels are real histograms: the backend publishes Micrometer
`http_server_requests_seconds_bucket` (percentile histograms + SLO buckets are
enabled in `application.yml`), and the ingress controller publishes
`nginx_ingress_controller_request_duration_seconds_bucket` (metrics enabled in
`infra/terraform/platform.tf`, scraped by `servicemonitor-ingress-nginx.yaml`).

## Alerts (see `docs/runbook.md` for responses)

| Alert                  | Condition                          | Severity |
| ---------------------- | ---------------------------------- | -------- |
| `BackendHigh5xxRate`   | 5xx > 5% of requests for 5m        | critical |
| `BackendHighP95Latency`| p95 > 300ms for 10m                | warning  |
| `BackendDbPoolExhausted` | threads queueing for a Hikari connection for 5m | critical |
| `BackendJvmHeapPressure` | JVM heap > 90% for 10m           | warning  |
| `ContainerMemoryNearLimit` | working set > 90% of memory limit for 10m | warning |
| `ContainerCpuThrottled`| CPU throttled in >25% of periods for 15m | warning |
| `HpaAtMaxReplicas`     | HPA pinned at maxReplicas for 15m  | warning  |
| `PodCrashLooping`      | >3 restarts in 15m                 | critical |
| `PodsUnschedulable`    | Pending pods for >10m              | warning  |

During the k6 load test (`load-test/`), watch the **HPA panel** on the platform
dashboard: replicas climb from 3 toward max as CPU crosses the 70% target.
