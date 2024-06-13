locals {
  # Thumbprints used for API testing
  allowed_test_certificate_thumbprints = [
    var.api_test_valid_certificate_thumbprint,
    var.api_test_expired_certificate_thumbprint,
    var.api_test_not_yet_valid_certificate_thumbprint,
  ]

  # List of thumbprints to be deployed in the APIM policy
  allowed_certificate_thumbprints = concat(
    local.allowed_test_certificate_thumbprints,
    var.allowed_client_certificate_thumbprints
  )

  api_mgmt_suffix = var.apim_suffix == "" ? var.env : var.apim_suffix
  api_mgmt_name   = "cft-api-mgmt-${local.api_mgmt_suffix}"
  api_mgmt_rg     = join("-", ["cft", var.env, "network-rg"])
}

module "cft_api_mgmt_product" {
  source                        = "git@github.com:hmcts/cnp-module-api-mgmt-product?ref=master"
  api_mgmt_name                 = local.api_mgmt_name
  api_mgmt_rg                   = local.api_mgmt_rg
  name                          = var.component
  product_access_control_groups = ["developers"]
  approval_required             = "false"
  subscription_required         = "true"

  providers = {
    azurerm = azurerm.aks-cftapps
  }
}

module "cft_api_mgmt" {
  source         = "git@github.com:hmcts/cnp-module-api-mgmt-api?ref=master"
  name           = "${var.component}-api"
  api_mgmt_name  = local.api_mgmt_name
  api_mgmt_rg    = local.api_mgmt_rg
  revision       = "1"
  product_id     = module.cft_api_mgmt_product.product_id
  display_name   = "Blob Router API"
  path           = "reform-scan"
  protocols      = ["http", "https"]
  service_url    = "http://${var.product}-${var.component}-${var.env}.service.core-compute-${var.env}.internal"
  swagger_url    = "https://hmcts.github.io/cnp-api-docs/specs/blob-router-service.json"
  content_format = "openapi-link"

  providers = {
    azurerm = azurerm.aks-cftapps
  }
}

module "cft_api_mgmt_policy" {
  source                 = "git@github.com:hmcts/cnp-module-api-mgmt-api-policy?ref=master"
  api_mgmt_name          = local.api_mgmt_name
  api_mgmt_rg            = local.api_mgmt_rg
  api_name               = module.cft_api_mgmt.name
  api_policy_xml_content = replace(file("cft-api-policy.xml"), "ALLOWED_CERTIFICATE_THUMBPRINTS", join(",", formatlist("&quot;%s&quot;", local.allowed_certificate_thumbprints)))

  providers = {
    azurerm = azurerm.aks-cftapps
  }
}
