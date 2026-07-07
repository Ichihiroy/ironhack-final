# ADR-0008: Pull-based GitOps with Argo CD (CI stops deploying)

- Status: accepted
- Date: 2026-07-07
- Amends: the deploy stages described in ADR-0004/ADR-0005 (build/scan/push
  stages unchanged)

## Context

Deploys were push-based: the app workflows ran `helm upgrade --atomic` against
AKS with the app-ci OIDC identity. That worked, but it meant (a) CI held
cluster credentials, (b) the running state lived only in the cluster and in
workflow logs — nothing in git said *what should currently be running*, and
(c) manual `kubectl` edits drifted silently until the next deploy.

## Decision

Argo CD runs in the cluster (installed by Terraform, `gitops.tf`) and
continuously reconciles git → cluster:

- **Charts** stay in `deploy/`; **desired state** (per-env image tags and
  overrides) lives in `gitops/values/*.yaml`.
- CI's deploy stages become **promote jobs**: after test → Trivy → push →
  cosign sign, the pipeline commits an `image.tag` bump to
  `gitops/values/<app>-staging.yaml`; the production bump runs behind the same
  reviewer-gated `production` GitHub environment as before. **CI never talks
  to AKS** — app-ci shrinks to AcrPush only.
- Split of knowledge, deliberate: git holds *what* runs (tags, replica
  overrides); Terraform injects *where/who* (ACR login server, workload
  identity client id, Key Vault name, tenant id) as Helm parameters in the
  Argo CD `Application` specs. This keeps the repo's "no real GUIDs in git"
  rule intact — which is also why the Applications are Terraform-managed
  rather than an in-git app-of-apps.
- `syncPolicy.automated` with `prune` + `selfHeal`: deletions in git prune the
  cluster, and manual cluster edits are reverted. Drift between git and
  cluster is now structurally impossible to miss.

## What replaces `--atomic`

`helm --atomic` rolled back a release whose probes failed. Under Argo CD the
equivalent is: automated sync with retry/backoff, health assessment marking
the Application `Degraded` when a rollout fails its probes, and rollback as a
one-line `git revert` of the tag bump (no cluster access needed). The old
RollingUpdate + readiness-probe safety net is unchanged — a bad image never
takes traffic either way.

## Consequences

- Rollback, audit, and disaster recovery are all git operations; `git log
  gitops/values/` *is* the deploy history.
- The promote jobs push to `main`; if branch protection is enabled later, the
  bot needs a bypass or the bumps become auto-merged PRs.
- `image.tag` starts empty, so Applications error ("image.tag is required")
  until each app pipeline runs once — expected first-boot state (GUIDE.md).
- The helm provider reads cluster credentials from the AKS resource in the
  same apply; tearing down cluster + releases in one destroy can need two
  passes.
- Rejected: Flux (fine tool; Argo CD's UI demos better for a capstone), an
  in-git app-of-apps (would force GUIDs into git or a templating layer), and
  keeping push-based helm (no reconciliation, CI keeps cluster credentials).
