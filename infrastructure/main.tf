provider "azurerm" {
  version = "=1.33.1"
}

locals {
  tags = "${merge(var.common_tags, map("Team Contact", "#rbs"))}"
}

resource "azurerm_resource_group" "rg" {
  name     = "${var.product}-${var.env}"
  location = "${var.location}"

  tags = "${local.tags}"
}

module "vault" {
  source                  = "git@github.com:hmcts/cnp-module-key-vault?ref=master"
  name                    = "${var.product}-${var.env}"
  product                 = "${var.product}"
  env                     = "${var.env}"
  tenant_id               = "${var.tenant_id}"
  object_id               = "${var.jenkins_AAD_objectId}"
  resource_group_name     = "${azurerm_resource_group.rg.name}"
  product_group_object_id = "70de400b-4f47-4f25-a4f0-45e1ee4e4ae3"
  common_tags             = "${local.tags}"
  location                = "${var.location}"

  managed_identity_object_id = "${var.managed_identity_object_id}"
}

data "azurerm_key_vault" "key_vault" {
  name                = "${module.vault.key_vault_name}"
  resource_group_name = "${azurerm_resource_group.rg.name}"
}

resource "azurerm_application_insights" "appinsights" {
  name                = "${var.product}-${var.env}"
  location            = "${var.location}"
  resource_group_name = "${azurerm_resource_group.rg.name}"
  application_type    = "${var.application_type}"
  tags                = "${var.common_tags}"
}

# store app insights key in key vault
resource "azurerm_key_vault_secret" "appinsights_secret" {
  name         = "app-insights-instrumentation-key"
  value        = "${azurerm_application_insights.appinsights.instrumentation_key}"
  key_vault_id = "${data.azurerm_key_vault.key_vault.id}"
}

# store blob storage secrets in key vault
resource "azurerm_key_vault_secret" "storage_account_name" {
  key_vault_id = "${data.azurerm_key_vault.key_vault.id}"
  name         = "storage-account-name"
  value        = "${azurerm_storage_account.storage_account.name}"
}

resource "azurerm_key_vault_secret" "storage_account_primary_key" {
  key_vault_id = "${data.azurerm_key_vault.key_vault.id}"
  name         = "storage-account-primary-key"
  value        = "${azurerm_storage_account.storage_account.primary_access_key}"
}

resource "azurerm_key_vault_secret" "storage_account_secondary_key" {
  key_vault_id = "${data.azurerm_key_vault.key_vault.id}"
  name         = "storage-account-secondary-key"
  value        = "${azurerm_storage_account.storage_account.secondary_access_key}"
}
