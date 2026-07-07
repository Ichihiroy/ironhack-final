provider "azurerm" {
  features {
    key_vault {
      purge_soft_delete_on_destroy    = false
      recover_soft_deleted_key_vaults = true
    }
  }

  # azurerm 4.x requires a subscription id; null falls back to ARM_SUBSCRIPTION_ID (CI).
  subscription_id = var.subscription_id
}

data "azurerm_client_config" "current" {}

# In-cluster add-ons (Argo CD, Kyverno) are installed by the same apply that
# creates the cluster, so the helm provider reads its connection straight from
# the AKS resource. Known caveat: destroying the cluster and its releases in
# one apply can fail — destroy releases first if ever tearing down piecemeal.
provider "helm" {
  kubernetes {
    host                   = azurerm_kubernetes_cluster.main.kube_config[0].host
    client_certificate     = base64decode(azurerm_kubernetes_cluster.main.kube_config[0].client_certificate)
    client_key             = base64decode(azurerm_kubernetes_cluster.main.kube_config[0].client_key)
    cluster_ca_certificate = base64decode(azurerm_kubernetes_cluster.main.kube_config[0].cluster_ca_certificate)
  }
}
