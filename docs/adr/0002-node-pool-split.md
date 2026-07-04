# ADR-0002: Separate system and user node pools, both autoscaled

- Status: accepted
- Date: 2026-07-04

## Context
AKS requires a system pool; workloads can share it or get their own pool. This
platform intentionally runs CPU-stress load tests (HPA demo).

## Decision
Two pools (`aks.tf`): `system` with `only_critical_addons_enabled = true`
(taints out app pods), and `user` where all workloads schedule via
`nodeSelector: workload=apps`. Cluster autoscaler enabled on both with explicit
min/max (system 2–3, user 2–6).

## Consequences
- A workload CPU spike (the k6 test does this deliberately) can exhaust the
  user pool without starving CoreDNS, metrics-server, or the CSI driver.
- Pools scale and upgrade independently; user-pool VM size can change without
  touching the system pool.
- Slightly higher base cost than one shared pool — accepted; the failure mode
  of a shared pool (control-plane addon starvation under load) is exactly what
  our demo would trigger.
