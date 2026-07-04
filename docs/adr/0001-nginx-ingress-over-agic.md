# ADR-0001: NGINX Ingress Controller + cert-manager (AGIC rejected)

- Status: accepted
- Date: 2026-07-04

## Context
The cluster needs one public L7 entry point serving the SPA at `/` and the API
at `/api`, with automated TLS. Azure offers AGIC (Application Gateway Ingress
Controller); the community standard is ingress-nginx.

## Decision
Deploy **ingress-nginx** with **cert-manager** (Let's Encrypt ClusterIssuer).
The frontend chart owns the single Ingress resource for both paths.

## Consequences
- Portable, massively documented, fast reconciliation; annotations cover
  rewrites/canary/rate-limits without touching Azure resources.
- cert-manager gives hands-off certificate issuance and renewal.
- **We give up the App Gateway WAF.** That is the one real loss, and AGIC (or
  Front Door + WAF in front of NGINX) is the documented upgrade path if
  OWASP-rule filtering becomes a requirement.
- Rejected AGIC because: config drift between cluster and App Gateway, slower
  reconciliation loops, Azure lock-in of routing config, and a second billing
  and ops surface — disproportionate for this system's threat model.
