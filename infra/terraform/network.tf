resource "azurerm_virtual_network" "main" {
  name                = "vnet-${local.name_prefix}"
  resource_group_name = data.azurerm_resource_group.main.name
  location            = data.azurerm_resource_group.main.location
  address_space       = [var.vnet_address_space]
  tags                = var.tags
}

# AKS nodes + pods (Azure CNI assigns pod IPs from this subnet)
resource "azurerm_subnet" "aks" {
  name                 = "snet-aks"
  resource_group_name  = data.azurerm_resource_group.main.name
  virtual_network_name = azurerm_virtual_network.main.name
  address_prefixes     = [var.snet_aks_cidr]
}

# Data plane: private endpoints only (Azure SQL). Isolated from node subnet
# so NSG/network policy boundaries stay clean.
resource "azurerm_subnet" "data" {
  name                 = "snet-data"
  resource_group_name  = data.azurerm_resource_group.main.name
  virtual_network_name = azurerm_virtual_network.main.name
  address_prefixes     = [var.snet_data_cidr]

  private_endpoint_network_policies = "Enabled"
}
