resource "azurerm_container_registry" "main" {
  name                = "acr${local.name_compact}"
  resource_group_name = data.azurerm_resource_group.main.name
  location            = data.azurerm_resource_group.main.location
  sku                 = "Standard"
  admin_enabled       = false # identity-based auth only; no shared admin credentials
  tags                = var.tags
}

# Nodes pull images with the kubelet identity — no imagePullSecrets anywhere.
resource "azurerm_role_assignment" "aks_acr_pull" {
  scope                            = azurerm_container_registry.main.id
  role_definition_name             = "AcrPull"
  principal_id                     = azurerm_kubernetes_cluster.main.kubelet_identity[0].object_id
  skip_service_principal_aad_check = true
}
