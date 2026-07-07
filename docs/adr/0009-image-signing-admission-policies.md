# ADR-0009: Cosign keyless signing + Kyverno admission policies

- Status: accepted
- Date: 2026-07-07
- Closes: the "signing is not yet enabled" gap documented in
  `security/SECURITY.md`

## Context

Trivy gates what gets *pushed*, and immutable sha tags pin what gets
*deployed*, but nothing proved the image admitted to the cluster was built by
*our* pipeline — a stolen AcrPush credential could push a poisoned image that
the cluster would happily run. Chart conventions (non-root, limits, ACR-only
images) were also just conventions: nothing rejected a pod that ignored them.

## Decision

**Signing (CI side).** Both app workflows sign every pushed image digest with
`cosign sign` in keyless mode: the GitHub OIDC token gets a short-lived Fulcio
certificate binding the signature to `repo:Ichihiroy/ironhack-final` on
`refs/heads/main`, logged in the public Rekor transparency log. No keys exist,
so none can leak or need rotation — the same reasoning as ADR-0004.

**Verification (cluster side).** Kyverno (installed by Terraform, policies
synced from `gitops/policies/` by Argo CD) enforces at admission, scoped to
`app-staging`/`app-production`:

| Policy | Action |
| ------ | ------ |
| `verify-image-signature` — cosign keyless, issuer = GitHub Actions, subject = this repo's workflows on main; `mutateDigest` pins the verified digest | **Audit**, flip to Enforce after validating registry auth |
| `disallow-latest-tag` (+ tag required) | Enforce |
| `restrict-registries` — `*.azurecr.io` only | Enforce |
| `require-run-as-nonroot` | Enforce |
| `require-resource-limits` | Enforce |

Signature verification starts as **Audit** because Kyverno must pull signature
artifacts from ACR (auth via a docker-registry secret wired to Kyverno's
`--imagePullSecrets`, or the kubelet identity's AcrPull through IMDS) — a
wrong guess in Enforce mode would block every deploy on a cluster we can't
test offline. The flip is a one-line change in
`gitops/policies/verify-image-signature.yaml` once a test pod admits cleanly.

## Consequences

- The supply chain is now closed end to end: commit → build → scan → push →
  **sign** → admission **verify** → run, every link pinned by digest.
- Rekor entries are public — fine here (public repo), a consideration for
  private forks.
- Kyverno is one more system component (runs on the user pool); its
  PolicyReports become the audit trail for violations.
- Rejected: key-based cosign (key management contradicts the no-secrets
  design), Gatekeeper/OPA (no native image verification — would need Ratify),
  and enforcing signature checks from day one (see Audit rationale above).
