# ============================================================================
# In-cluster platform add-ons (ingress, TLS, observability)
#
# Installed by the SAME apply that creates the cluster, for the same reason
# Argo CD and Kyverno are (gitops.tf): the training subscription is wiped
# nightly, so anything installed by hand dies with it. One infra-apply run
# must bring the whole platform back — GUIDE.md "morning kickstart".
#
# What runs (Prometheus rules, ServiceMonitor, Grafana dashboards, network
# policies) still lives in git and is synced by Argo CD (see gitops.tf);
# Terraform only installs the operators/controllers those manifests need.
# ============================================================================

resource "helm_release" "ingress_nginx" {
  name             = "ingress-nginx"
  namespace        = "ingress-nginx"
  create_namespace = true
  repository       = "https://kubernetes.github.io/ingress-nginx"
  chart            = "ingress-nginx"
  version          = "4.15.1"
  timeout          = 600

  depends_on = [azurerm_kubernetes_cluster_node_pool.user]
}

resource "helm_release" "cert_manager" {
  name             = "cert-manager"
  namespace        = "cert-manager"
  create_namespace = true
  repository       = "https://charts.jetstack.io"
  chart            = "cert-manager"
  version          = "v1.21.0"
  timeout          = 600

  values = [yamlencode({
    crds = { enabled = true }
  })]

  depends_on = [azurerm_kubernetes_cluster_node_pool.user]
}

# Grafana admin password: generated here, stored ONLY in Key Vault (never in
# git, never in a values file). Retrieve with:
#   az keyvault secret show --vault-name <kv> --name grafana-admin-password
resource "random_password" "grafana_admin" {
  length  = 24
  special = false # avoids quoting surprises when pasted into a login form
}

resource "azurerm_key_vault_secret" "grafana_admin" {
  name         = "grafana-admin-password"
  value        = random_password.grafana_admin.result
  key_vault_id = azurerm_key_vault.main.id

  depends_on = [azurerm_role_assignment.deployer_kv_officer]
}

resource "helm_release" "kube_prometheus_stack" {
  name             = "kps"
  namespace        = "monitoring"
  create_namespace = true
  repository       = "https://prometheus-community.github.io/helm-charts"
  chart            = "kube-prometheus-stack"
  version          = "87.15.1"
  timeout          = 900 # CRDs + operator + prometheus: slowest of the add-ons

  values = [
    file("${path.module}/../../observability/kube-prometheus-stack-values.yaml"),
    yamlencode({
      grafana = { adminPassword = random_password.grafana_admin.result }
    }),
  ]

  depends_on = [azurerm_kubernetes_cluster_node_pool.user]
}
