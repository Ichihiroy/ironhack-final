resource "azurerm_log_analytics_workspace" "main" {
  name                = "log-${local.name_prefix}"
  resource_group_name = data.azurerm_resource_group.main.name
  location            = data.azurerm_resource_group.main.location
  sku                 = "PerGB2018"
  retention_in_days   = 30
  tags                = var.tags
}

# Risk-accepted (two IaC findings), both consequences of the single-subscription
# student platform's access model — documented here so the acceptance is auditable:
#
#  AZU-0041 (no api_server_access_profile allow-list): `terraform apply` and the
#    in-cluster add-on installs run from the GitHub Actions runner via the helm
#    provider, whose egress IPs are dynamic and unbounded — an IP allow-list would
#    lock CI (and the operator's port-forward) out of the API server.
#  AZU-0042 (no managed Azure AD RBAC): Kubernetes RBAC is always enabled on v4
#    clusters; Azure AD RBAC/SSO is deferred because the Terraform helm provider
#    authenticates with the admin kube_config and there is a single operator
#    (port-forward access, GUIDE.md). Enabling it requires switching the provider
#    to kube_admin_config and provisioning an AAD admin group — tracked as
#    multi-operator hardening. See security/SECURITY.md.
#trivy:ignore:AVD-AZU-0041 exp:2027-01-01
#trivy:ignore:AVD-AZU-0042 exp:2027-01-01
resource "azurerm_kubernetes_cluster" "main" {
  name                = "aks-${local.name_prefix}"
  resource_group_name = data.azurerm_resource_group.main.name
  location            = data.azurerm_resource_group.main.location
  dns_prefix          = "aks-${local.name_prefix}"
  kubernetes_version  = var.kubernetes_version
  tags                = var.tags

  # Workload identity: pods exchange projected SA tokens for Entra tokens —
  # no secrets in the cluster for Azure access (see docs/adr).
  oidc_issuer_enabled       = true
  workload_identity_enabled = true

  identity {
    type = "SystemAssigned"
  }

  # System pool: tainted to critical addons only, so app workloads land on the
  # user pool and control-plane components stay isolated.
  default_node_pool {
    name                         = "system"
    vm_size                      = var.system_node_vm_size
    vnet_subnet_id               = azurerm_subnet.aks.id
    only_critical_addons_enabled = true

    auto_scaling_enabled = true
    min_count            = var.system_node_min_count
    max_count            = var.system_node_max_count

    upgrade_settings {
      max_surge = "10%"
    }
  }

  network_profile {
    network_plugin = "azure"
    network_policy = "azure" # required for the NetworkPolicies in security/
    service_cidr   = "172.16.0.0/16"
    dns_service_ip = "172.16.0.10"
  }

  # Container insights → Log Analytics
  oms_agent {
    log_analytics_workspace_id = azurerm_log_analytics_workspace.main.id
  }

  # Key Vault secrets via CSI driver (SecretProviderClass in security/)
  key_vault_secrets_provider {
    secret_rotation_enabled  = true
    secret_rotation_interval = "2m"
  }
}

# User pool: all application workloads; autoscaled independently of the system pool.
resource "azurerm_kubernetes_cluster_node_pool" "user" {
  name                  = "user"
  kubernetes_cluster_id = azurerm_kubernetes_cluster.main.id
  vm_size               = var.user_node_vm_size
  vnet_subnet_id        = azurerm_subnet.aks.id
  mode                  = "User"
  tags                  = var.tags

  auto_scaling_enabled = true
  min_count            = var.user_node_min_count
  max_count            = var.user_node_max_count

  node_labels = {
    workload = "apps"
  }
}
