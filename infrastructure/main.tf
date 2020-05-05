provider "azurerm" {
  version = "=1.42.0"
}

locals {
  reform-scan-vault-name = "reform-scan-${var.env}"
  bulk-scan-vault-name   = "bulk-scan-${var.env}"
}

module "reform-blob-router-db" {
  source             = "git@github.com:hmcts/cnp-module-postgres?ref=master"
  product            = "${var.component}"
  location           = "${var.location_db}"
  env                = "${var.env}"
  database_name      = "blob_router"
  postgresql_user    = "blob_router"
  postgresql_version = "11"
  sku_name           = "GP_Gen5_2"
  sku_tier           = "GeneralPurpose"
  common_tags        = "${var.common_tags}"
  subscription       = "${var.subscription}"
}

# Staging DB to be used by AAT staging pod for functional tests
module "reform-blob-router-staging-db" {
  source             = "git@github.com:hmcts/cnp-module-postgres?ref=master"
  product            = "${var.component}-staging"
  location           = "${var.location_db}"
  env                = "${var.env}"
  database_name      = "blob_router"
  postgresql_user    = "blob_router"
  postgresql_version = "11"
  sku_name           = "GP_Gen5_2"
  sku_tier           = "GeneralPurpose"
  common_tags        = "${var.common_tags}"
  subscription       = "${var.subscription}"
}

# region: key vault definitions

data "azurerm_key_vault" "bulk_scan_key_vault" {
  name                = "${local.bulk-scan-vault-name}"
  resource_group_name = "bulk-scan-${var.env}"
}

data "azurerm_key_vault" "reform_scan_key_vault" {
  name                = "${local.reform-scan-vault-name}"
  resource_group_name = "reform-scan-${var.env}"
}

# endregion

# region: storage secrets from bulk scan

data "azurerm_key_vault_secret" "bulk_scan_storage_connection_string" {
  key_vault_id = "${data.azurerm_key_vault.bulk_scan_key_vault.id}"
  name         = "storage-account-connection-string"
}

# endregion

# region: copy CFT storage account secrets from bulk-scan key vault to reform-scan key vault

resource "azurerm_key_vault_secret" "bulkscan_storage_connection_string" {
  name         = "bulkscan-storage-connection-string"
  value        = "${data.azurerm_key_vault_secret.bulk_scan_storage_connection_string.value}"
  key_vault_id = "${data.azurerm_key_vault.reform_scan_key_vault.id}"
}

# endregion

# region: error notification secrets from bulk scan

data "azurerm_key_vault_secret" "bulk_scan_error_notifications_password" {
  key_vault_id = "${data.azurerm_key_vault.bulk_scan_key_vault.id}"
  name         = "error-notifications-password"
}

data "azurerm_key_vault_secret" "bulk_scan_error_notifications_url" {
  key_vault_id = "${data.azurerm_key_vault.bulk_scan_key_vault.id}"
  name         = "error-notifications-url"
}

data "azurerm_key_vault_secret" "bulk_scan_error_notifications_username" {
  key_vault_id = "${data.azurerm_key_vault.bulk_scan_key_vault.id}"
  name         = "error-notifications-username"
}

# endregion

# region: copy error notification secrets from bulk scan to reform scan

resource "azurerm_key_vault_secret" "error_notifications_password" {
  name         = "error-notifications-password"
  value        = "${data.azurerm_key_vault_secret.bulk_scan_error_notifications_password.value}"
  key_vault_id = "${data.azurerm_key_vault.reform_scan_key_vault.id}"
}

resource "azurerm_key_vault_secret" "error_notifications_url" {
  name         = "error-notifications-url"
  value        = "${data.azurerm_key_vault_secret.bulk_scan_error_notifications_url.value}"
  key_vault_id = "${data.azurerm_key_vault.reform_scan_key_vault.id}"
}

resource "azurerm_key_vault_secret" "error_notifications_username" {
  name         = "error-notifications-username"
  value        = "${data.azurerm_key_vault_secret.bulk_scan_error_notifications_username.value}"
  key_vault_id = "${data.azurerm_key_vault.reform_scan_key_vault.id}"
}

# endregion

# region DB secrets
resource "azurerm_key_vault_secret" "POSTGRES-USER" {
  name         = "${var.component}-POSTGRES-USER"
  key_vault_id = "${data.azurerm_key_vault.reform_scan_key_vault.id}"
  value        = "${module.reform-blob-router-db.user_name}"
}

resource "azurerm_key_vault_secret" "POSTGRES-PASS" {
  name         = "${var.component}-POSTGRES-PASS"
  key_vault_id = "${data.azurerm_key_vault.reform_scan_key_vault.id}"
  value        = "${module.reform-blob-router-db.postgresql_password}"
}

resource "azurerm_key_vault_secret" "POSTGRES_HOST" {
  name         = "${var.component}-POSTGRES-HOST"
  key_vault_id = "${data.azurerm_key_vault.reform_scan_key_vault.id}"
  value        = "${module.reform-blob-router-db.host_name}"
}

resource "azurerm_key_vault_secret" "POSTGRES_PORT" {
  name         = "${var.component}-POSTGRES-PORT"
  key_vault_id = "${data.azurerm_key_vault.reform_scan_key_vault.id}"
  value        = "${module.reform-blob-router-db.postgresql_listen_port}"
}

resource "azurerm_key_vault_secret" "POSTGRES_DATABASE" {
  name         = "${var.component}-POSTGRES-DATABASE"
  key_vault_id = "${data.azurerm_key_vault.reform_scan_key_vault.id}"
  value        = "${module.reform-blob-router-db.postgresql_database}"
}
# endregion

# region reports secrets

data "azurerm_key_vault_secret" "bulk_scan_reports_email_username" {
  key_vault_id = "${data.azurerm_key_vault.bulk_scan_key_vault.id}"
  name         = "reports-email-username"
}

data "azurerm_key_vault_secret" "bulk_scan_reports_email_password" {
  key_vault_id = "${data.azurerm_key_vault.bulk_scan_key_vault.id}"
  name         = "reports-email-password"
}

data "azurerm_key_vault_secret" "bulk_scan_reports_recipients" {
  key_vault_id = "${data.azurerm_key_vault.bulk_scan_key_vault.id}"
  name         = "reports-recipients"
}

# endregion

# region: copy reports secrets from bulk scan to reform scan

resource "azurerm_key_vault_secret" "reports_email_username" {
  name         = "reports-email-username"
  value        = "${data.azurerm_key_vault_secret.bulk_scan_reports_email_username.value}"
  key_vault_id = "${data.azurerm_key_vault.reform_scan_key_vault.id}"
}

resource "azurerm_key_vault_secret" "reports_email_password" {
  name         = "reports-email-password"
  value        = "${data.azurerm_key_vault_secret.bulk_scan_reports_email_password.value}"
  key_vault_id = "${data.azurerm_key_vault.reform_scan_key_vault.id}"
}

resource "azurerm_key_vault_secret" "reports_recipients" {
  name         = "reports-recipients"
  value        = "${data.azurerm_key_vault_secret.bulk_scan_reports_recipients.value}"
  key_vault_id = "${data.azurerm_key_vault.reform_scan_key_vault.id}"
}

# endregion
