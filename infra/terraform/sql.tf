resource "random_password" "sql_admin" {
  length      = 32
  special     = true
  min_upper   = 2
  min_lower   = 2
  min_numeric = 2
  min_special = 2
}

resource "azurerm_mssql_server" "main" {
  name                          = "sql-${local.name_prefix}"
  resource_group_name           = data.azurerm_resource_group.main.name
  location                      = data.azurerm_resource_group.main.location
  version                       = "12.0"
  administrator_login           = var.sql_admin_login
  administrator_login_password  = random_password.sql_admin.result
  minimum_tls_version           = "1.2"
  public_network_access_enabled = false # reachable only through the private endpoint
  tags                          = var.tags
}

resource "azurerm_mssql_database" "main" {
  name      = "appdb"
  server_id = azurerm_mssql_server.main.id
  sku_name  = var.sql_database_sku
  tags      = var.tags
}

# ── Private endpoint + DNS so pods resolve sql-*.database.windows.net privately ──
resource "azurerm_private_dns_zone" "sql" {
  name                = "privatelink.database.windows.net"
  resource_group_name = data.azurerm_resource_group.main.name
  tags                = var.tags
}

resource "azurerm_private_dns_zone_virtual_network_link" "sql" {
  name                  = "pdnsl-${local.name_prefix}-sql"
  resource_group_name   = data.azurerm_resource_group.main.name
  private_dns_zone_name = azurerm_private_dns_zone.sql.name
  virtual_network_id    = azurerm_virtual_network.main.id
  tags                  = var.tags
}

resource "azurerm_private_endpoint" "sql" {
  name                = "pe-${local.name_prefix}-sql"
  resource_group_name = data.azurerm_resource_group.main.name
  location            = data.azurerm_resource_group.main.location
  subnet_id           = azurerm_subnet.data.id
  tags                = var.tags

  private_service_connection {
    name                           = "psc-${local.name_prefix}-sql"
    private_connection_resource_id = azurerm_mssql_server.main.id
    subresource_names              = ["sqlServer"]
    is_manual_connection           = false
  }

  private_dns_zone_group {
    name                 = "default"
    private_dns_zone_ids = [azurerm_private_dns_zone.sql.id]
  }
}

# ── App connection secrets → Key Vault (consumed via CSI SecretProviderClass) ──
locals {
  sql_jdbc_url = "jdbc:sqlserver://${azurerm_mssql_server.main.fully_qualified_domain_name}:1433;database=${azurerm_mssql_database.main.name};encrypt=true;trustServerCertificate=false;loginTimeout=30;"
}

resource "azurerm_key_vault_secret" "sql_url" {
  name         = "sql-datasource-url"
  value        = local.sql_jdbc_url
  key_vault_id = azurerm_key_vault.main.id

  depends_on = [azurerm_role_assignment.deployer_kv_officer]
}

resource "azurerm_key_vault_secret" "sql_username" {
  name         = "sql-datasource-username"
  value        = var.sql_admin_login
  key_vault_id = azurerm_key_vault.main.id

  depends_on = [azurerm_role_assignment.deployer_kv_officer]
}

resource "azurerm_key_vault_secret" "sql_password" {
  name         = "sql-datasource-password"
  value        = random_password.sql_admin.result
  key_vault_id = azurerm_key_vault.main.id

  depends_on = [azurerm_role_assignment.deployer_kv_officer]
}
