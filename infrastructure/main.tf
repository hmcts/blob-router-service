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
  name                    = "${product}-${var.env}"
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
