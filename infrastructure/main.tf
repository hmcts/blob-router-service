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

resource "azurerm_application_insights" "appinsights" {
  name                = "${var.product}-${var.env}"
  location            = "${var.location}"
  resource_group_name = "${azurerm_resource_group.rg.name}"
  application_type    = "${var.application_type}"
  tags                = "${var.common_tags}"
}
