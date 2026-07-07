# gitops/ — desired state watched by Argo CD

Pull-based GitOps (ADR-0008): Argo CD runs in the cluster and continuously
reconciles what this directory (plus the Helm charts under `deploy/`) says
should be running. **CI never talks to the cluster** — its only deploy power is
a git commit here.

```
gitops/
├── values/     # per-environment Helm values, one file per app × env
│   ├── backend-staging.yaml       # image.tag written by CI (promote-staging)
│   ├── backend-production.yaml    # image.tag written by CI behind the reviewer gate
│   ├── frontend-staging.yaml
│   └── frontend-production.yaml
└── policies/   # Kyverno ClusterPolicies, synced by the kyverno-policies Application
    ├── verify-image-signature.yaml   # cosign keyless — ADR-0009 (Audit → Enforce)
    ├── disallow-latest-tag.yaml
    ├── restrict-registries.yaml
    ├── require-run-as-nonroot.yaml
    └── require-resource-limits.yaml
```

## Who writes what

| Writer | What | How |
|---|---|---|
| CI promote jobs | `values/*.yaml` → `image.tag` | commit to main after tests + Trivy + push + cosign; production bump sits behind the reviewer-gated `production` environment |
| Humans (via PR) | any other override in `values/`, any policy | normal review flow; Argo CD syncs on merge |
| Terraform (`infra/terraform/gitops.tf`) | the Argo CD `Application` specs themselves | cluster identifiers (ACR login server, workload-identity client id, Key Vault name, tenant id) are injected as Helm parameters at apply time — **never commit GUIDs here** |

## Day-2 operations

- **Rollback**: `git revert` the offending tag-bump commit; Argo CD converges.
  No cluster access needed.
- **Drift**: `selfHeal: true` reverts manual `kubectl` edits automatically;
  what's in git is what runs.
- **First deploy**: `image.tag` starts empty, so the app Applications show a
  helm render error ("image.tag is required") until each app pipeline has run
  once on main. That's expected — see GUIDE.md ordering.
