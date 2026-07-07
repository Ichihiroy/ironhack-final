# Ironhack Final — Production-Shaped 3-Tier Platform on AKS

A monorepo delivering a 3-tier application (React + Vite / Spring Boot /
Azure SQL) on Azure Kubernetes Service with **fully automated infrastructure**,
**two independent CI/CD pipelines**, **observability**, and **security
hardening**. The application layer is a deliberately thin, swappable stub —
the deliverable is the platform (see
[docs/architecture.md](docs/architecture.md)).

> **Getting started?** [GUIDE.md](GUIDE.md) is the ordered checklist of every
> human step from local repo → provisioned platform → graded demo.

```text
apps/        frontend (React+Vite+TS → unprivileged nginx)  ·  backend (Spring Boot 3 / Java 21)
infra/       bootstrap (Phase 0, run-once)  ·  terraform (AKS, ACR, KV, SQL, VNet, Log Analytics)
deploy/      Helm charts: probes, HPA, PDB, spread, NetworkPolicy, SecretProviderClass
gitops/      Argo CD desired state: per-env image tags (written by CI) + Kyverno policies
observability/  kube-prometheus-stack values, ServiceMonitor, alerts, Grafana dashboards
security/    default-deny NetworkPolicies, RBAC, CSI reference, SECURITY.md
load-test/   k6 — 350 req/s sustained, p95 ≤ 300ms, trips the HPA on camera
.github/     infra-plan · infra-apply · infra-drift · frontend/backend-ci-cd · secret-scan · dependabot (all OIDC, no secrets)
docs/        architecture (mermaid + why), runbook, 9 ADRs
```

## How this repo operates: Phase 0, then pull requests forever

1. **Phase 0 (manual, once)** — a human with subscription Owner runs
   [`infra/bootstrap/bootstrap.sh`](infra/bootstrap/README.md). It creates the
   only things CI cannot create for itself: the Terraform state storage, the
   app resource group (RBAC anchor), and three OIDC-federated CI identities
   (plan-RO / apply-RW / app-CI). No secrets are produced — only IDs.
2. **Everything after** is a PR:
   - `infra/**` PR → `infra-plan` posts the Terraform plan on the PR →
     merge → `infra-apply` waits for approval on the `production` environment
     → applies.
   - `apps/backend/**` or `apps/frontend/**` PR → tests + build + Trivy gate →
     merge → image pushed as immutable `sha-<gitsha>` + **cosign-signed** →
     CI commits the staging tag bump to `gitops/values/` → approval → the
     production bump. **Argo CD** (in-cluster) reconciles each bump — CI never
     touches AKS ([ADR-0008](docs/adr/0008-pull-based-gitops-argocd.md)).

## Exact setup steps

```bash
# 0. Prereqs: az CLI (logged in as Owner), a GitHub repo of this code
# 1. Phase 0 (once)
cd infra/bootstrap
SUBSCRIPTION_ID=<sub-id> GITHUB_REPO=<org>/<repo> ./bootstrap.sh

# 2. GitHub repo config (once)
#    - Settings → Environments: create 'staging' and 'production';
#      add a required reviewer on 'production'
#    - Settings → Actions → Variables: paste the variables the script printed

# 3. Provision infra: open a PR touching infra/ (even a whitespace commit),
#    review the plan comment, merge, approve the gated apply.

# 4. Post-apply repo variable (once): ACR_LOGIN_SERVER (value: terraform output)
#    Argo CD + Kyverno were installed by the same apply; cluster identifiers
#    are wired into the Argo Application specs by Terraform, not repo vars.

# 5. In-cluster platform (once): ingress-nginx, cert-manager, monitoring, policies
az aks get-credentials -g rg-thelocals-ironhack-prod -n aks-thelocals-ironhack-prod
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm upgrade --install ingress-nginx ingress-nginx/ingress-nginx \
  -n ingress-nginx --create-namespace
helm repo add jetstack https://charts.jetstack.io
helm upgrade --install cert-manager jetstack/cert-manager \
  -n cert-manager --create-namespace --set crds.enabled=true
# monitoring stack + dashboards + alerts:
cd observability && cat README.md
kubectl apply -f security/networkpolicies.yaml   # after first app deploy creates namespaces

# 6. Ship the apps: push any change under apps/backend/ or apps/frontend/ to main.
```

Local development needs none of the above:

```bash
docker compose up --build     # frontend :8081, backend :8080, postgres
```

(Local uses Postgres for convenience; prod is Azure SQL — the app only sees
`SPRING_DATASOURCE_*` env vars either way.)

## The demo: scalability + observability in one run

```bash
k6 run -e BASE_URL=https://<host> load-test/items-load.js
```

350 req/s sustained against `/api/items`, asserting p95 ≤ 300 ms, while the
Grafana **Platform** dashboard shows HPA replicas climbing 3 → max and back.
Details: [load-test/README.md](load-test/README.md).

## Rubric map

| Rubric line | Where it is satisfied |
| ----------- | --------------------- |
| **Architecture** — design & justification | [docs/architecture.md](docs/architecture.md) (mermaid + every decision with the rejected alternative), [docs/adr/](docs/adr/) 0001–0009 |
| **Infra automation** — IaC, no clicks | [infra/terraform/](infra/terraform/) (AKS system+user autoscaled pools, ACR, Key Vault, Azure SQL + private endpoint, VNet, Log Analytics, workload identity), [infra/bootstrap/](infra/bootstrap/) for the documented run-once Phase 0 |
| **Infra CI/CD** — plan on PR, gated apply, policy checks | [.github/workflows/infra-plan.yml](.github/workflows/infra-plan.yml) (fmt, validate, Trivy IaC policy scan, plan-as-PR-comment), [.github/workflows/infra-apply.yml](.github/workflows/infra-apply.yml) (OIDC RO/RW split, `production` reviewer gate); state locking in [infra/bootstrap/README.md](infra/bootstrap/README.md), automated nightly drift detection in [.github/workflows/infra-drift.yml](.github/workflows/infra-drift.yml) + [docs/runbook.md](docs/runbook.md#infra-drift-detection) |
| **App CI/CD** — two independent path-filtered pipelines | [.github/workflows/backend-ci-cd.yml](.github/workflows/backend-ci-cd.yml), [.github/workflows/frontend-ci-cd.yml](.github/workflows/frontend-ci-cd.yml) (test → build → Trivy gate → sha-tag push + cosign sign → GitOps promote to staging → gated production promote) |
| **GitOps** — pull-based deploys, git as source of truth | [gitops/](gitops/), Argo CD installed by [infra/terraform/gitops.tf](infra/terraform/gitops.tf), [docs/adr/0008](docs/adr/0008-pull-based-gitops-argocd.md) (self-heal, prune, `git revert` rollback) |
| **Kubernetes quality** — probes, HPA, PDB, spread, resources | [deploy/backend/](deploy/backend/), [deploy/frontend/](deploy/frontend/) (helm lint + template clean) |
| **Security** — secrets, identity, network, supply chain | [security/SECURITY.md](security/SECURITY.md) (index of all controls), [security/networkpolicies.yaml](security/networkpolicies.yaml), [security/rbac.yaml](security/rbac.yaml), Key Vault CSI in [deploy/backend/templates/secretproviderclass.yaml](deploy/backend/templates/secretproviderclass.yaml), Trivy gates in both app workflows, cosign signing + Kyverno admission policies ([docs/adr/0009](docs/adr/0009-image-signing-admission-policies.md), [gitops/policies/](gitops/policies/)), gitleaks secret scan ([.github/workflows/secret-scan.yml](.github/workflows/secret-scan.yml)), Dependabot + SHA-pinned actions ([.github/dependabot.yml](.github/dependabot.yml)) |
| **Observability** — metrics, dashboards, alerts | [observability/](observability/) (kube-prometheus-stack values, ServiceMonitor → `/actuator/prometheus`, 2 Grafana dashboards, 4 Alertmanager rules), responses in [docs/runbook.md](docs/runbook.md) |
| **Scalability** — HPA + cluster autoscaler, proven | HPAs in both charts, autoscaled node pools in [infra/terraform/aks.tf](infra/terraform/aks.tf), demo in [load-test/](load-test/) |
| **Operations** — deploy, rollback, rotation, incidents | [docs/runbook.md](docs/runbook.md) |

## Swappability guarantee

The future product replaces `apps/` code while keeping the contract (port 8080,
actuator probe paths, `GET /api/items`-style API surface, `SPRING_DATASOURCE_*`
env vars, runtime `env.js`). Pipelines rebuild, rescan, and redeploy via
`image.tag=sha-<gitsha>` — Terraform, Helm plumbing, workflows, dashboards, and
alerts are untouched. See the contract table in
[docs/architecture.md](docs/architecture.md#the-swappable-app-contract).
