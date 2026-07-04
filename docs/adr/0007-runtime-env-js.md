# ADR-0007: Frontend runtime configuration via env.js (no build-time baking)

- Status: accepted
- Date: 2026-07-04

## Context
Vite inlines `VITE_*` variables at build time, which would weld the API URL
into the bundle and force a distinct image per environment — incompatible with
ADR-0005's promote-the-same-sha model.

## Decision
`index.html` loads `/env.js` before the bundle. The container entrypoint
(`docker-entrypoint.d/10-runtime-env.sh`) rewrites that file from the
`VITE_API_URL` env var at startup; nginx serves it with `Cache-Control:
no-store`. The Helm chart sets the env var per environment (empty = same-origin
through the shared ingress).

## Consequences
- One frontend image serves staging and production; config travels in Helm
  values, not images.
- The API URL is runtime configuration, not a secret — no Key Vault involved.
- One extra tiny HTTP request before the bundle executes; negligible, and the
  no-store header guarantees env changes take effect on refresh.
- Rejected: `import.meta.env.VITE_API_URL` baking — per-env images; and
  nginx-side `sub_filter` templating — more magic, same result.
