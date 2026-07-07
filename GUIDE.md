# GUIDE — From local repo to graded demo

The ordered checklist of every human step. Everything not listed here is
automated. Deeper detail lives in [README.md](README.md),
[infra/bootstrap/README.md](infra/bootstrap/README.md), and
[docs/runbook.md](docs/runbook.md).

## 1. Push the repo to GitHub

Create the GitHub repository (under the team org, so teammates have access),
then:

```bash
git add .
git commit -m "Initial platform: infra, pipelines, charts, observability, security, docs"
git remote add origin https://github.com/<org>/<repo>.git
git push -u origin main
```

## 2. Configure GitHub (Settings, ~5 minutes)

- **Environments** (Settings → Environments):
  - Create `staging` (no protection needed).
  - Create `production` and add a **required reviewer** — a teammate, since you
    may not be able to approve your own deploys. This gate is what the whole
    RW-identity security model depends on (see `docs/adr/0004`).
- **Branch protection** on `main` (Settings → Rules → Rulesets): require pull
  requests and passing status checks. The rubric explicitly grades "push to
  main (protected)". **Important**: add the `github-actions` app as a bypass
  actor on the ruleset — the GitOps promote jobs (ADR-0008) commit image-tag
  bumps to `gitops/values/` on main and will fail without it.

## 3. Phase 0 bootstrap (once, by a teammate with subscription Owner)

```bash
az login
cd infra/bootstrap
SUBSCRIPTION_ID=<sub-id> GITHUB_REPO=<org>/<repo> ./bootstrap.sh
```

- Optional env vars: `TEAM` (default `thelocals`), `PROJECT` (`ironhack`),
  `ENVIRONMENT` (`prod`), `LOCATION` (`westeurope`). **`TEAM` must match the
  `team` Terraform variable.**
- The script is idempotent — safe to re-run.
- Paste the ~9 printed values into **Settings → Secrets and variables →
  Actions → Variables**. They are identifiers, not secrets.

## 4. Provision infrastructure through the pipeline (never from a laptop)

1. Open a PR touching `infra/**` (editing a comment is enough).
2. Confirm on the PR: the Terraform **plan appears as a comment** and the
   **Trivy IaC policy scan** passes.
3. Merge → in the Actions run, **approve** the `production`-gated apply.
   First apply takes ~15–20 min (AKS + SQL are slow). The same apply installs
   **Argo CD** and **Kyverno** into the cluster and registers the app
   `Application` specs (`infra/terraform/gitops.tf`) — no manual GitOps setup.
4. From the apply job's `terraform output` (or a local `terraform output`
   against remote state), set the last repo variable: `ACR_LOGIN_SERVER`.
   (Cluster identifiers are injected into Argo CD by Terraform — ADR-0008.)

## 5. Install the in-cluster platform (once, manual)

```bash
az aks get-credentials -g rg-thelocals-ironhack-prod -n aks-thelocals-ironhack-prod

# Ingress + TLS
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm upgrade --install ingress-nginx ingress-nginx/ingress-nginx \
  -n ingress-nginx --create-namespace
helm repo add jetstack https://charts.jetstack.io
helm upgrade --install cert-manager jetstack/cert-manager \
  -n cert-manager --create-namespace --set crds.enabled=true
# Then create a ClusterIssuer named `letsencrypt-prod` — needs a DNS name
# pointed at the ingress controller's public IP first.

# Monitoring stack, dashboards, alert rules
cd observability && cat README.md   # follow the install + dashboard-loading steps
```

After the first app deploy has created the namespaces:

```bash
kubectl apply -f security/networkpolicies.yaml
```

Set the repo variables `FRONTEND_HOST_STAGING` / `FRONTEND_HOST_PRODUCTION`
once DNS exists (the ingress renders host-less until then). They flow into the
Argo CD Application specs as `TF_VAR`s, so run one more infra PR → gated apply
for the hosts to take effect.

## 6. Ship the apps

Push (or PR + merge) any change under `apps/backend/**` and
`apps/frontend/**`. Pipeline per app: test → image build → Trivy gate →
push `sha-<gitsha>` + cosign sign → commit staging tag bump to
`gitops/values/` → **approve** → production tag bump. Argo CD picks each bump
up and syncs the cluster (within ~3 min, or press Refresh in its UI).

Until this first run, the Argo CD apps show a helm error ("image.tag is
required") — expected: git doesn't know a deployable image yet.

Watch Argo CD reconcile (optional but demo-worthy):

```bash
kubectl -n argocd port-forward svc/argocd-server 8082:80 &
# login: admin / $(kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath='{.data.password}' | base64 -d)
```

Verify staging before approving production (from `docs/runbook.md`):

```bash
kubectl -n app-staging rollout status deploy/backend
kubectl -n app-staging port-forward svc/backend 8080:8080 &
curl -s http://localhost:8080/actuator/health/readiness   # {"status":"UP"}
```

Readiness includes the DB check, so `UP` proves the whole
Key Vault → CSI → Secret → env-var → Azure SQL chain.

## 7. Rehearse the demo (Scalability + Observability in one run)

```bash
k6 run -e BASE_URL=https://<host> load-test/items-load.js
```

While it runs (~9 min), show:

1. Grafana → **Platform** dashboard → HPA panel: replicas step 3 → max.
2. Grafana → **Backend API** dashboard: ~350 req/s with p95 under 300 ms.
3. `kubectl -n app-staging get hpa backend --watch` in a terminal.

**Do a dry run before the presentation** — the HPA takes a few minutes to
react and ~5 more to scale back down; know the timing.

## Known first-run gotchas

- **Trivy gate fails on a fresh CVE** in a base image → bump the base image
  tag in the Dockerfile. That is the gate working as designed — say so in the
  demo if it happens.
- **Let's Encrypt rate limits** → use the LE *staging* issuer while testing
  cert-manager; switch the ClusterIssuer to prod when DNS and routing are
  stable.
- **OIDC login fails in a workflow** → the job is probably running outside the
  federated subject it was trusted for (e.g. a deploy job moved out of its
  `environment:` block). Subjects are listed in `infra/bootstrap/bootstrap.sh`.
- **First infra PR fails at init** → repo variables from step 3 missing or
  misnamed.
- **Promote job can't push to main** → the branch-protection ruleset is
  missing the `github-actions` bypass actor (step 2).
- **Kyverno signature policy**: ships in **Audit** mode. After the first
  deploy, check `kubectl get policyreports -n app-staging` — once reports show
  the signature passing, flip `failureAction: Audit` → `Enforce` in
  `gitops/policies/verify-image-signature.yaml` (see ADR-0009 for the
  registry-auth caveat).

## Defense-day talking points

- **NGINX over AGIC** (deliberate, ADR-0001): the trade-off we accepted is
  losing the App Gateway WAF; AGIC/Front Door is the documented upgrade path.
- **Unit tests only in the backend stub**: integration tests arrive with the
  real product — the stub has no logic to integration-test, and keeping CI
  hermetic was the point. Say it proactively.
- **"Key Vault access policies"** in the rubric: we use the newer, better
  mechanism — RBAC-authorization mode (`rbac_authorization_enabled = true`).
- **RG created in bootstrap, not Terraform** (ADR-0006): least-privilege RBAC
  scoping requires the scope to exist before CI identities can be granted
  roles on it.
- **Pull-based GitOps** (ADR-0008): CI holds zero cluster credentials; deploys
  are commits to `gitops/values/`, Argo CD reconciles with self-heal, rollback
  is `git revert`. Demo it live: `kubectl -n app-staging scale deploy/backend
  --replicas=1` and watch Argo CD put it back.
- **Supply chain closed end to end** (ADR-0009): Trivy gate → immutable sha
  tag → cosign keyless signature → Kyverno admission verification; plus
  gitleaks, Dependabot, and SHA-pinned actions on the repo side.
- The product idea is still open **by design**: the app layer is a swappable
  stub; the platform (this repo) is the deliverable. Swapping = change
  `apps/`, keep the contract (see `docs/architecture.md`).
