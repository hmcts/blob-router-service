locals {
  api_mgmt_suffix = var.apim_suffix == "" ? var.env : var.apim_suffix
  api_mgmt_name   = "cft-api-mgmt-${local.api_mgmt_suffix}"
  api_mgmt_rg     = join("-", ["cft", var.env, "network-rg"])
}

data "template_file" "apim_policy" {
  template = file("${path.module}/api-mgmt-policy.xml")

  vars = {
    tenant_id = data.azurerm_key_vault_secret.apim_tenant_id.value
    client_id = data.azurerm_key_vault_secret.apim_client_id.value
    app_id    = data.azurerm_key_vault_secret.apim_app_id.value
    api_key   = data.azurerm_key_vault_secret.api_key.value
  }
}

module "api_mgmt_product" {
  source                        = "git@github.com:hmcts/cnp-module-api-mgmt-product?ref=master"
  api_mgmt_name                 = local.api_mgmt_name
  api_mgmt_rg                   = local.api_mgmt_rg
  name                          = "bulk-scan"
  product_access_control_groups = ["developers"]
  approval_required             = "false"
  subscription_required         = "false"

  providers = {
    azurerm = azurerm.aks-cftapps
  }
}

module "api_mgmt" {
  source                = "git@github.com:hmcts/cnp-module-api-mgmt-api?ref=master"
  name                  = "bulk-scan-api"
  api_mgmt_name         = local.api_mgmt_name
  api_mgmt_rg           = local.api_mgmt_rg
  revision              = "2"
  product_id            = module.api_mgmt_product.product_id
  display_name          = "Bulk Scan API"
  path                  = "bulk-scan"
  protocols             = ["http", "https"]
  service_url           = "http://${var.product}-${var.component}-${var.env}.service.core-compute-${var.env}.internal"
  swagger_url           = "https://hmcts.github.io/cnp-api-docs/specs/blob-router-service.json"
  content_format        = "openapi-link"
  subscription_required = "false"

  providers = {
    azurerm = azurerm.aks-cftapps
  }
}

module "api_mgmt_policy" {
  source                 = "git@github.com:hmcts/cnp-module-api-mgmt-api-policy?ref=master"
  api_mgmt_name          = local.api_mgmt_name
  api_mgmt_rg            = local.api_mgmt_rg
  api_name               = module.api_mgmt.name
  api_policy_xml_content = data.template_file.apim_policy.rendered

  providers = {
    azurerm = azurerm.aks-cftapps
  }
}