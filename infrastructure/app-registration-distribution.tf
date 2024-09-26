# Moves app registrations used for the APIM from the central KV to one of bulk scans

data "azurerm_key_vault" "source_kv" {
  provider = azurerm.mgmt
  name                = "central-app-reg-kv"
  resource_group_name = "central-app-registration-rg"
}

data "azurerm_key_vault" "destination_kv" {
  name                = "reform-scan-${var.env}"
  resource_group_name = "reform-scan-${var.env}"
}

# Get secrets from source KV
data "azurerm_key_vault_secret" "source_secret" {
  for_each     = toset(var.secret_names)
  name         = each.key
  key_vault_id = data.azurerm_key_vault.source_kv.id
}

# Create secrets copied over in bulk scan KV
resource "azurerm_key_vault_secret" "destination_secrets" {
  for_each = toset(var.secret_names)

  name         = each.key
  value        = data.azurerm_key_vault_secret.source_secret[each.key].value
  key_vault_id = data.azurerm_key_vault.destination_kv.id
}