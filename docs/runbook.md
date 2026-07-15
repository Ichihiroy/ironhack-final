# Runbook

Operational procedures for the AKS platform. All commands assume
`az aks get-credentials --resource-group rg-thelocals-ironhack-prod --name aks-thelocals-ironhack-prod`
has been run by someone with cluster access.

## Deploy

Normal path — **never deploy by hand**:

1. Merge a PR touching `apps/backend/**` or `apps/frontend/**` into `main`.
2. The pipeline tests, builds, Trivy-scans, pushes `sha-<gitsha>`, cosign-signs
   the digest, commits the staging tag bump to `gitops/values/`, then waits for
   approval on the `production` environment before committing the production
   bump. Argo CD reconciles each bump into the cluster (ADR-0008) — CI itself
   never touches AKS.
3. Verify staging before approving (Argo CD UI shows sync/health too —
   `kubectl -n argocd port-forward svc/argocd-server 8082:80`):

   ```bash
   kubectl -n app-staging rollout status deploy/backend
   kubectl -n app-staging port-forward svc/backend 8080:8080 &
   curl -s http://localhost:8080/actuator/health/readiness   # {"status":"UP"}
   ```

4. Approve the `production` promotion in the GitHub Actions run; Argo CD syncs
   `app-production` from the resulting commit.

Infra path: PR touching `infra/**` → review the plan comment → merge → approve
the gated apply.

## Infra drift detection

The only supported write path to Azure is `infra-apply` on main — nobody has
standing Contributor access, so drift should be rare and is always suspicious.
To detect it:

1. Open a PR touching `infra/**` (a comment-only change is enough) — the plan
   posted on the PR IS the drift report: any resource listed as changing that
   no commit touched was modified out of band.
2. Investigate via the Azure Activity Log on the resource group (who/what/when)
   before deciding direction:
   - Codify: replicate the change in Terraform and merge, or
   - Revert: merge an empty-change PR and let the gated apply reconcile
     reality back to code.

This is automated: `.github/workflows/infra-drift.yml` runs
`terraform plan -detailed-exitcode` nightly (06:00 UTC) with the read-only
platform identity, opens/refreshes a GitHub issue labeled `drift` when live
state diverges, and fails the run so it shows red. bootstrap.sh grants
platform-ro the extra `ref:refs/heads/main` federated credential the scheduled
context needs. The manual PR route above still works for on-demand checks
(or run the workflow via `workflow_dispatch`).

**App-layer drift** needs no detection at all: Argo CD `selfHeal` reverts any
manual change to the app namespaces within seconds — git is the only write
path (ADR-0008).

## Rollback

**Safety net**: rollouts are RollingUpdate behind readiness probes — pods that
fail probes never take traffic, and Argo CD marks the Application `Degraded`
so the failure is loud (Argo UI / `kubectl -n argocd get applications`).

**Rollback = git revert** (no cluster access needed):

```bash
git log --oneline -- gitops/values/           # deploy history
git revert <bad-tag-bump-commit> && git push  # Argo CD converges within ~3min
```

Or edit `gitops/values/<app>-production.yaml` back to the previous
`sha-<gitsha>` and push. Because tags are immutable (ADR-0005), the
rolled-back pods are bit-for-bit the previously scanned and signed image.
Emergency-only alternative: press "History and rollback" in the Argo CD UI —
then still make git match, or `selfHeal` will re-apply the bad version.

Infra rollback: `git revert` the infra commit → PR → plan → gated apply.

## Secret rotation (DB credentials)

1. Rotate in the source of truth:
   - Terraform way: taint the password —
     `terraform taint random_password.sql_admin` → PR/merge → gated apply
     updates both the SQL server and the Key Vault secrets.
   - Break-glass way: reset in Azure SQL, then update the Key Vault secrets
     `sql-datasource-password` (new version) by hand; reconcile Terraform later.
2. The CSI driver re-pulls Key Vault every 2 minutes
   (`secret_rotation_interval` in `aks.tf`) and updates the synced Secret.
3. Env vars are read at process start, so restart pods to pick it up:

   ```bash
   kubectl -n app-production rollout restart deploy/backend
   ```

4. Verify: readiness returns UP (readiness includes the DB check, so a bad
   credential shows up immediately as NotReady — before traffic is affected).

## Scale

- **App, temporarily** (overrides HPA until next deploy):
  `kubectl -n app-production scale deploy/backend --replicas=6`
- **App, durably**: raise `autoscaling.maxReplicas` (or resources) in
  `deploy/backend/values.yaml` via PR.
- **Cluster**: raise `user_node_max_count` in `infra/terraform` via PR → gated
  apply. The cluster autoscaler handles the rest.

## Incident response — the alerts

### High 5xx rate (`BackendHigh5xxRate`, critical)
1. Scope it: Grafana → Backend API dashboard — all URIs or one? One namespace or both?
2. `kubectl -n app-production logs deploy/backend --since=15m | grep -iE "error|exception" | head -50`
3. Readiness DOWN with DB errors → check SQL: Azure portal (throttling, DTU
   cap, failover) and Key Vault secret versions (mid-rotation mismatch — see
   secret rotation above).
4. Started right after a deploy → **rollback** (above); the deploy is the
   suspect until proven otherwise.

### Elevated p95 latency (`BackendHighP95Latency`, warning)
1. Platform dashboard: is the HPA already at `maxReplicas`? Are user-pool
   nodes saturated? → raise max / node count (Scale, above).
2. Not CPU? Check DB-side waits and connection-pool saturation
   (`hikaricp_connections_*` metrics on the backend dashboard datasource).
3. If load is a runaway client, apply rate limiting at the NGINX ingress
   (annotation) while investigating.

### CrashLoopBackOff (`PodCrashLooping`, critical)
1. `kubectl -n <ns> describe pod <pod>` — exit code and last state.
   `kubectl -n <ns> logs <pod> --previous | tail -50`
2. OOMKilled (137) → raise memory limits via values PR; crash on boot with
   secret/config errors → check the CSI mount events
   (`kubectl -n <ns> get events | grep -i secret`) and Key Vault access.
3. Bad image/config from a fresh deploy → rollback.

### Unschedulable pods (`PodsUnschedulable`, warning)
1. `kubectl -n <ns> get pods --field-selector=status.phase=Pending` →
   `kubectl describe pod` → the `FailedScheduling` event says why.
2. Insufficient CPU/memory → cluster autoscaler should be adding nodes; if the
   user pool is at `max_count`, raise it (Scale, above).
3. Selector/taint mismatch (e.g. missing `workload=apps` label on new pools) →
   fix the pool/values, not the pod.

### DB pool exhausted (`BackendDbPoolExhausted`, critical)
Threads are queueing for connections — every queued request adds latency and
eventually times out.
1. Backend API dashboard → *DB connection pool*: is `active` pinned at `max`
   while `pending` climbs? That's demand, not a leak — check whether request
   rate (or the k6 `db_reads` scenario) just grew.
2. Flat traffic but pool pinned → slow queries holding connections: check
   Azure SQL metrics (DTU cap, blocking) in the portal.
3. Mitigate: scale replicas (each pod brings its own pool), then right-size
   `spring.datasource.hikari.maximum-pool-size` via values PR — respect the
   Azure SQL tier's connection ceiling across ALL pods.

### JVM heap pressure (`BackendJvmHeapPressure`, warning)
1. Backend API dashboard → *JVM heap used / max*: steady climb that never
   drops after GC = leak; sawtooth near the top = undersized heap.
2. Leak suspicion → capture before the OOM:
   `kubectl -n <ns> exec <pod> -- jcmd 1 GC.heap_dump /tmp/heap.hprof`
3. Undersized → raise the container memory limit via values PR (the JVM sizes
   its heap from the cgroup limit).

### Memory near limit (`ContainerMemoryNearLimit`, warning)
The next allocation spike gets the container OOMKilled (exit 137) — this alert
is the early warning for `PodCrashLooping`.
1. Platform dashboard → *Saturation — memory vs limit*: one pod or all?
   All replicas trending together = the limit is simply too small for the
   workload → raise `resources.limits.memory` via values PR.
2. One pod diverging → likely a leak; see JVM heap pressure above.

### CPU throttling (`ContainerCpuThrottled`, warning)
The container wants more CPU than its limit; the kernel is inserting pauses —
this inflates p95 even though node CPU looks fine.
1. Platform dashboard → *Saturation — CPU throttling ratio*: which container?
2. Sustained under normal traffic → raise `resources.limits.cpu` (or requests,
   so the HPA target reflects reality) via values PR.
3. During a load test this firing briefly is expected — it's the signal the
   HPA acts on; only act if it persists after scale-out settles.

### HPA at max (`HpaAtMaxReplicas`, warning)
Scaling headroom is gone: more load now degrades latency instead of adding
pods.
1. `kubectl -n <ns> get hpa backend` — current CPU vs target. At max AND above
   target = genuinely saturated; at max but below target = it will scale down
   soon, no action.
2. Raise `autoscaling.maxReplicas` via values PR; check user-pool
   `max_count` has node headroom for the extra pods (Scale, above).

## Useful one-liners

```bash
kubectl -n app-production get pods -o wide            # spread across nodes
kubectl -n app-production get hpa --watch             # live HPA during load test
kubectl top pods -n app-production                    # quick saturation check
helm -n app-production list                           # releases + revisions
kubectl -n app-production get events --sort-by=.lastTimestamp | tail -20
```
