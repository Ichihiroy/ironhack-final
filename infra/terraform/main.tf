locals {
  # Team prefix first: several teams deploy this stack into the same
  # subscription/region, and ACR/Key Vault/SQL names are globally unique.
  name_prefix = "${var.team}-${var.project}-${var.environment}"
  # Storage-account-style names: lowercase alphanumeric, length-capped.
  # Cap 21 keeps "kv-" + compact within Key Vault's 24-char limit.
  name_compact = substr(replace(local.name_prefix, "-", ""), 0, 21)
}

# The RG is created in Phase 0 (infra/bootstrap/bootstrap.sh), NOT here: the CI
# identities' least-privilege roles (Contributor / Reader / AcrPush) are scoped to
# this RG, and an RBAC scope must exist before roles can be assigned to it.
# Subscription-scoped grants were rejected — see docs/adr/0006.
data "azurerm_resource_group" "main" {
  name = "rg-${local.name_prefix}"
}
