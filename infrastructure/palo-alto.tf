locals {
  trusted_vnet_name           = "core-infra-vnet-${var.env}"
  trusted_vnet_resource_group = "core-infra-${var.env}"
  trusted_vnet_subnet_name    = "palo-trusted"
}

module "palo_alto" {
  source       = "git@github.com:hmcts/cnp-module-palo-alto?ref=master"
  subscription = "${var.subscription}"
  env          = "${var.env}"
  product      = "${var.product}"
  common_tags  = "${var.common_tags}"

  untrusted_vnet_name           = "core-infra-vnet-${var.env}"
  untrusted_vnet_resource_group = "core-infra-${var.env}"
  untrusted_vnet_subnet_name    = "palo-untrusted"
  trusted_vnet_name             = "${local.trusted_vnet_name}"
  trusted_vnet_resource_group   = "${local.trusted_vnet_resource_group}"
  trusted_vnet_subnet_name      = "${local.trusted_vnet_subnet_name}"
  trusted_destination_host      = "${azurerm_storage_account.storage_account.name}.blob.core.windows.net"
}
