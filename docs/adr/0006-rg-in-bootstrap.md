# ADR-0006: Application resource group created in bootstrap, consumed by Terraform as a data source

- Status: accepted
- Date: 2026-07-04

## Context
The brief wants (a) CI roles least-privilege-scoped to the resource group, not
the subscription, and (b) Terraform owning infrastructure including "the RG".
These conflict: Azure RBAC can only assign roles on a scope that already
exists, and the CI identities must receive their RG-scoped roles in Phase 0 —
before any Terraform has run.

## Decision
`bootstrap.sh` creates `rg-<team>-<project>-<env>` as the **RBAC scope anchor** and
assigns the three CI identities their roles on it. Terraform references the RG
via `data "azurerm_resource_group"` and manages everything inside it.

## Consequences
- CI identities never hold subscription-level rights; blast radius is the RG.
- The RG (and role assignments on it) live outside Terraform state — a small,
  clearly documented exception owned by Phase 0, which is already the home of
  the other unavoidable manual steps (tfstate backend, OIDC identities).
- Rejected: subscription-scoped Contributor (violates least privilege), and
  Terraform-managed RG with a first-run `terraform import` dance in CI
  (fragile, confusing failure modes).
