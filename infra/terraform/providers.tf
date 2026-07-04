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
