locals {
  # certificate thumbprints used by API tests
  allowed_test_certificate_thumbprints = [
    var.api_test_valid_certificate_thumbprint,
    var.api_test_expired_certificate_thumbprint,
    var.api_test_not_yet_valid_certificate_thumbprint,
  ]

  # list of the thumbprints of the SSL certificates that should be accepted by the API (gateway)
  allowed_certificate_thumbprints = concat(
    local.allowed_test_certificate_thumbprints,
    var.allowed_client_certificate_thumbprints
  )

  formatted_thumbprints = formatlist("&quot;%s&quot;", local.allowed_certificate_thumbprints)
  quoted_thumbprints    = join(",", local.formatted_thumbprints)
}

module "api_mgmt_product" {
  source        = "git@github.com:hmcts/cnp-module-api-mgmt-product?ref=master"
  api_mgmt_name = "core-api-mgmt-${var.env}"
  api_mgmt_rg   = "core-infra-${var.env}"
  name          = var.component
}

module "api_mgmt" {
  source        = "git@github.com:hmcts/cnp-module-api-mgmt-api?ref=master"
  name          = "${var.component}-api"
  api_mgmt_name = "core-api-mgmt-${var.env}"
  api_mgmt_rg   = "core-infra-${var.env}"
  revision      = "1"
  product_id    = module.api_mgmt_product.product_id
  display_name  = "Blob Router API"
  path          = "reform-scan"
  protocols = [
    "https"
  ]
  service_url = "http://${var.product}-${var.component}-${var.env}.service.core-compute-${var.env}.internal"
  swagger_url = "https://hmcts.github.io/reform-api-docs/specs/blob-router-service.json"
}

module "api_mgmt_policy" {
  source                 = "git@github.com:hmcts/cnp-module-api-mgmt-api-policy?ref=master"
  api_mgmt_name          = "core-api-mgmt-${var.env}"
  api_mgmt_rg            = "core-infra-${var.env}"
  api_name               = module.api_mgmt.name
  api_policy_xml_content = replace(file("api-policy.xml"), "ALLOWED_CERTIFICATE_THUMBPRINTS", local.quoted_thumbprints)
}
