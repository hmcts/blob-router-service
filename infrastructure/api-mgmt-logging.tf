resource "azurerm_api_management_api_diagnostic" "apim_logs" {
  provider                 = azurerm.aks-cftapps
  identifier               = "applicationinsights"
  resource_group_name      = local.api_mgmt_rg
  api_management_name      = local.api_mgmt_name
  api_name                 = "bulk-scan-api"
  api_management_logger_id = "/subscriptions/${var.cft_subscription_id}/resourceGroups/${local.api_mgmt_rg}/providers/Microsoft.ApiManagement/service/${local.api_mgmt_name}/loggers/cft-api-mgmt-${local.api_mgmt_suffix}-logger"

  sampling_percentage       = 100.0
  always_log_errors         = true
  log_client_ip             = true
  verbosity                 = "verbose"
  http_correlation_protocol = "W3C"

  frontend_request {
    body_bytes = 8192
    headers_to_log = [
      "content-type",
      "accept",
      "origin",
    ]
  }

  frontend_response {
    body_bytes = 8192
    headers_to_log = [
      "content-type",
      "content-length",
      "origin",
    ]
  }

  backend_request {
    body_bytes = 8192
    headers_to_log = [
      "content-type",
      "accept",
      "origin",
    ]
  }

  backend_response {
    body_bytes = 8192
    headers_to_log = [
      "content-type",
      "content-length",
      "origin",
    ]
  }

  depends_on = [
    module.api_mgmt
  ]
}