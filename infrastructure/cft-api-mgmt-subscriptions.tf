# Subscription keys for the CFT APIM

# Internal subscription - Bulk Scan DTS Team
resource "azurerm_api_management_subscription" "bulk_scan_team_subscription" {
  api_management_name = local.api_mgmt_name
  resource_group_name = local.api_mgmt_rg
  product_id          = module.cft_api_mgmt_product.id
  display_name        = "Blob Router API - Bulk Scan DTS Team Subscription"
  state               = "active"
  provider            = azurerm.aks-cftapps
}

resource "azurerm_key_vault_secret" "bulk_scan_team_subscription_key" {
  name         = "bulk-scan-team-cft-apim-subscription-key"
  value        = azurerm_api_management_subscription.bulk_scan_team_subscription.primary_key
  key_vault_id = data.azurerm_key_vault.reform_scan_key_vault.id
}

# Supplier subscription - Exela 
resource "azurerm_api_management_subscription" "exela_supplier_subscription" {
  api_management_name = local.api_mgmt_name
  resource_group_name = local.api_mgmt_rg
  product_id          = module.cft_api_mgmt_product.id
  display_name        = "Blob Router API - Exela Supplier Subscription"
  state               = "active"
  provider            = azurerm.aks-cftapps
}

resource "azurerm_key_vault_secret" "exela_supplier_subscription_key" {
  name         = "exela-cft-apim-subscription-key"
  value        = azurerm_api_management_subscription.exela_supplier_subscription.primary_key
  key_vault_id = data.azurerm_key_vault.reform_scan_key_vault.id
}
