# ADR-0003: Azure SQL with public access disabled + private endpoint

- Status: accepted
- Date: 2026-07-04

## Context
The database is the crown jewel. Azure SQL is reachable publicly by default,
tempered by firewall rules.

## Decision
`public_network_access_enabled = false`. A private endpoint in `snet-data`
plus the `privatelink.database.windows.net` private DNS zone (linked to the
VNet) makes the server's FQDN resolve to a private IP for pods (`sql.tf`).

## Consequences
- No public listener exists at all; a leaked connection string is unusable
  from outside the VNet. NetworkPolicy further restricts which pods may open
  1433.
- Migrations/admin tooling must run from inside the network (a CI job on the
  cluster, a jumpbox, or temporary VPN) — documented in the runbook.
- Rejected: firewall allow-lists and "Allow Azure services" — both keep a
  public endpoint alive and degrade into IP-hygiene chores.
