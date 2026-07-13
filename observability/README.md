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
| `backend-api.json`     | Request rate, 5xx error rate, p50/p95/p99 latency, saturation |
| `platform.json`        | Pod/node CPU+memory, **HPA activity**, restarts, deploy status |

## Alerts (see `docs/runbook.md` for responses)

| Alert                  | Condition                          | Severity |
| ---------------------- | ---------------------------------- | -------- |
| `BackendHigh5xxRate`   | 5xx > 5% of requests for 5m        | critical |
| `BackendHighP95Latency`| p95 > 300ms for 10m                | warning  |
| `PodCrashLooping`      | >3 restarts in 15m                 | critical |
| `PodsUnschedulable`    | Pending pods for >10m              | warning  |

During the k6 load test (`load-test/`), watch the **HPA panel** on the platform
dashboard: replicas climb from 3 toward max as CPU crosses the 70% target.
