provider "azurerm" {
  version = "=1.33.1"
}

locals {
  reform-scan-vault-name = "reform-scan-${var.env}"
  bulk-scan-vault-name = "bulk-scan-${var.env}"
}

data "azurerm_key_vault" "bulk-scan_key_vault" {
  name                = "${local.bulk-scan-vault-name}"
  resource_group_name = "bulk-scan-${var.env}"
}

data "azurerm_key_vault_secret" "cft_storage_account_name" {
  key_vault_id = "${data.azurerm_key_vault.bulk-scan_key_vault.id}"
  name      = "storage-account-name"
}

data "azurerm_key_vault_secret" "cft_storage_account_primary_key" {
  key_vault_id = "${data.azurerm_key_vault.bulk-scan_key_vault.id}"
  name      = "storage-account-primary-key"
}

# Copy CFT storage account secrets from bulk-scan key vault to reform-scan key vault
data "azurerm_key_vault" "reform_scan_key_vault" {
  name = "${local.reform-scan-vault-name}"
  resource_group_name = "reform-scan-${var.env}"
}

resource "azurerm_key_vault_secret" "cft_storage_account_name" {
  name         = "bulk-scan-storage-account-name"
  value        = "${data.azurerm_key_vault_secret.cft_storage_account_name.value}"
  key_vault_id = "${data.azurerm_key_vault.reform_scan_key_vault.id}"
}

resource "azurerm_key_vault_secret" "cft_storage_account_primary_key" {
  name         = "bulk-scan-storage-account-primary-key"
  value        = "${data.azurerm_key_vault_secret.cft_storage_account_primary_key.value}"
  key_vault_id = "${data.azurerm_key_vault.reform_scan_key_vault.id}"
}
