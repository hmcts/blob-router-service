locals {
  vault_name             = "reform-scan-${var.env}"
  reform-scan-vault-name = "reform-scan-${var.env}"
  bulk-scan-vault-name   = "bulk-scan-${var.env}"
  s2s-vault-name         = "s2s-${var.env}"
  resource_group_name    = "${var.product}-${var.env}"
  db_host_name           = "${var.component}-flexible-postgres-db-v15"
  db_name                = "blob_router"
  postgresql_user        = "api_user"
}

# region: key vault definitions

data "azurerm_key_vault" "bulk_scan_key_vault" {
  name                = local.bulk-scan-vault-name
  resource_group_name = "bulk-scan-${var.env}"
}

data "azurerm_key_vault" "reform_scan_key_vault" {
  name                = local.reform-scan-vault-name
  resource_group_name = "reform-scan-${var.env}"
}

data "azurerm_key_vault" "s2s_key_vault" {
  name                = local.s2s-vault-name
  resource_group_name = "rpe-service-auth-provider-${var.env}"
}

# endregion

# region: storage secrets from bulk scan

data "azurerm_key_vault_secret" "bulk_scan_storage_connection_string" {
  key_vault_id = data.azurerm_key_vault.bulk_scan_key_vault.id
  name         = "storage-account-connection-string"
}

# endregion

# region: blob-router s2s secret from s2s vault

data "azurerm_key_vault_secret" "s2s_secret" {
  key_vault_id = data.azurerm_key_vault.s2s_key_vault.id
  name         = "microservicekey-reform-scan-blob-router"
}

# endregion

# region: copy s2s secret to reform-scan key vault

resource "azurerm_key_vault_secret" "blob_router_s2s_secret" {
  name         = "s2s-secret-blob-router"
  value        = data.azurerm_key_vault_secret.s2s_secret.value
  key_vault_id = data.azurerm_key_vault.reform_scan_key_vault.id
}

# endregion

# region: copy CFT storage account secrets from bulk-scan key vault to reform-scan key vault

resource "azurerm_key_vault_secret" "bulkscan_storage_connection_string" {
  name         = "bulkscan-storage-connection-string"
  value        = data.azurerm_key_vault_secret.bulk_scan_storage_connection_string.value
  key_vault_id = data.azurerm_key_vault.reform_scan_key_vault.id
}

# endregion

# region: error notification secrets from bulk scan

data "azurerm_key_vault_secret" "bulk_scan_error_notifications_password" {
  key_vault_id = data.azurerm_key_vault.bulk_scan_key_vault.id
  name         = "error-notifications-password"
}

data "azurerm_key_vault_secret" "bulk_scan_error_notifications_url" {
  key_vault_id = data.azurerm_key_vault.bulk_scan_key_vault.id
  name         = "error-notifications-url"
}

data "azurerm_key_vault_secret" "bulk_scan_error_notifications_username" {
  key_vault_id = data.azurerm_key_vault.bulk_scan_key_vault.id
  name         = "error-notifications-username"
}

# endregion

# region: copy error notification secrets from bulk scan to reform scan

resource "azurerm_key_vault_secret" "error_notifications_password" {
  name         = "error-notifications-password"
  value        = data.azurerm_key_vault_secret.bulk_scan_error_notifications_password.value
  key_vault_id = data.azurerm_key_vault.reform_scan_key_vault.id
}

resource "azurerm_key_vault_secret" "error_notifications_url" {
  name         = "error-notifications-url"
  value        = data.azurerm_key_vault_secret.bulk_scan_error_notifications_url.value
  key_vault_id = data.azurerm_key_vault.reform_scan_key_vault.id
}

resource "azurerm_key_vault_secret" "error_notifications_username" {
  name         = "error-notifications-username"
  value        = data.azurerm_key_vault_secret.bulk_scan_error_notifications_username.value
  key_vault_id = data.azurerm_key_vault.reform_scan_key_vault.id
}

# endregion

# region reports secrets

data "azurerm_key_vault_secret" "bulk_scan_reports_email_username" {
  key_vault_id = data.azurerm_key_vault.bulk_scan_key_vault.id
  name         = "reports-email-username"
}

data "azurerm_key_vault_secret" "bulk_scan_reports_email_password" {
  key_vault_id = data.azurerm_key_vault.bulk_scan_key_vault.id
  name         = "reports-email-password"
}

data "azurerm_key_vault_secret" "bulk_scan_reports_recipients" {
  key_vault_id = data.azurerm_key_vault.bulk_scan_key_vault.id
  name         = "reports-recipients"
}

# endregion

# region: copy reports secrets from bulk scan to reform scan

resource "azurerm_key_vault_secret" "reports_email_username" {
  name         = "reports-email-username"
  value        = data.azurerm_key_vault_secret.bulk_scan_reports_email_username.value
  key_vault_id = data.azurerm_key_vault.reform_scan_key_vault.id
}

resource "azurerm_key_vault_secret" "reports_email_password" {
  name         = "reports-email-password"
  value        = data.azurerm_key_vault_secret.bulk_scan_reports_email_password.value
  key_vault_id = data.azurerm_key_vault.reform_scan_key_vault.id
}

resource "azurerm_key_vault_secret" "reports_recipients" {
  name         = "reports-recipients"
  value        = data.azurerm_key_vault_secret.bulk_scan_reports_recipients.value
  key_vault_id = data.azurerm_key_vault.reform_scan_key_vault.id
}

data "azurerm_key_vault_secret" "apim_app_id" {
  key_vault_id = data.azurerm_key_vault.reform_scan_key_vault.id
  name         = "bulk-scan-app-id"
}

data "azurerm_key_vault_secret" "apim_client_id" {
  key_vault_id = data.azurerm_key_vault.reform_scan_key_vault.id
  name         = "bulk-scan-client-id"
}

data "azurerm_key_vault_secret" "apim_tenant_id" {
  key_vault_id = data.azurerm_key_vault.reform_scan_key_vault.id
  name         = "tenant-id"
}

data "azurerm_key_vault_secret" "api_key" {
  key_vault_id = data.azurerm_key_vault.reform_scan_key_vault.id
  name         = "reconciliation-api-key"
}