# Observability

Prometheus + Grafana + Alertmanager via **kube-prometheus-stack**, scraping the
backend's Micrometer endpoint (`/actuator/prometheus`).

## Install (once per cluster, after infra apply)

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm upgrade --install kps prometheus-community/kube-prometheus-stack \
  --namespace monitoring --create-namespace \
  -f kube-prometheus-stack-values.yaml \
  --set grafana.adminPassword="$(openssl rand -base64 24)"   # or your own; never commit one

kubectl apply -f servicemonitor-backend.yaml
kubectl apply -f prometheus-rules.yaml
```

## Load the dashboards

The Grafana sidecar watches for ConfigMaps labeled `grafana_dashboard=1`:

```bash
for d in dashboards/*.json; do
  name=$(basename "$d" .json)
  kubectl create configmap "dash-$name" -n monitoring --from-file="$d" \
    --dry-run=client -o yaml | kubectl apply -f -
  kubectl label configmap "dash-$name" -n monitoring grafana_dashboard=1 --overwrite
done
```

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
