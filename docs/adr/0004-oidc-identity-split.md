# ADR-0004: GitHub OIDC with split RO / RW / App identities

- Status: accepted
- Date: 2026-07-04

## Context
CI must authenticate to Azure for terraform plan (every PR), terraform apply
(main only), image pushes, and Helm deploys. Static service-principal secrets
are the common but weakest option.

## Decision
Three Entra app registrations, all **federated to GitHub OIDC** (no client
secrets anywhere), created in Phase 0 (`bootstrap.sh`):

| Identity      | Roles (scoped to the app RG)                       | Federated subject |
| ------------- | -------------------------------------------------- | ----------------- |
| platform-ro   | Reader (+ tfstate blob access)                     | `pull_request` |
| platform-rw   | Contributor + RBAC Administrator (+ tfstate blob)  | `environment:production` only |
| app-ci        | AcrPush + AKS Cluster User                         | `ref:refs/heads/main`, `environment:staging`, `environment:production` |

## Consequences
- Nothing to leak or rotate; trust is the repo+subject claim, verified by Azure
  per-job with a 10-minute token.
- The RW credential is *cryptographically unusable* outside the
  reviewer-gated `production` environment — PR authors can read plans, never
  apply.
- RBAC Administrator (not Owner) is required because Terraform itself creates
  role assignments (AcrPull, Key Vault roles) and Contributor may not grant
  roles; scoping it to the RG bounds the blast radius.
- Rejected: one almighty SP with a secret in GitHub — single point of total
  compromise, plus rotation toil.
