# ADR-0005: Immutable sha-<gitsha> image tags as the deploy source of truth

- Status: accepted
- Date: 2026-07-04

## Context
Something must connect "what is running" to "what was built and scanned".
Moving tags (`latest`, `main`) break that link.

## Decision
CI pushes every image as `<acr>/<app>:sha-<gitsha>` and deploys **only** that
tag (`helm --set image.tag=sha-<gitsha>`). A moving `:main` alias is pushed
for developer convenience and is never referenced by any deploy.

## Consequences
- Any running pod maps to one commit, one pipeline run, one Trivy report.
- Rollback = redeploy the previous sha (or `helm rollback`); no registry state
  can change what a tag means mid-incident.
- Registry accumulates tags; an ACR retention task (or periodic purge of
  untagged/old manifests) is a documented follow-up, not a blocker.
- Rejected: deploying `:main` — irreproducible deploys, ambiguous rollbacks,
  and a scan gate that certifies a tag whose content later changes.
