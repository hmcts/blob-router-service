locals {
  flexible_secret_prefix = "${var.component}-POSTGRES-FLEXIBLE"

  flexible_secrets = [
    {
      name_suffix = "PASS"
      value       = module.postgresql.password
    },
    {
      name_suffix = "HOST"
      value       = module.postgresql.fqdn
    },
    {
      name_suffix = "USER"
      value       = module.postgresql.username
    },
    {
      name_suffix = "PORT"
      value       = "5432"
    },
    {
      name_suffix = "DATABASE"
      value       = local.db_name
    }
  ]

}

locals {
  flexible_secret_prefix_staging = "${var.component}-staging-flexible-db"

  flexible_secrets_staging = var.env == prod ? [{}] : [
    {
      name_suffix = "password"
      value       = module.postgresql_staging[0].password
      count       = var.num_staging_dbs
    },
    {
      name_suffix = "host"
      value       = module.postgresql_staging[0].fqdn
      count       = var.num_staging_dbs
    },
    {
      name_suffix = "user"
      value       = module.postgresql_staging[0].username
      count       = var.num_staging_dbs
    },
    {
      name_suffix = "port"
      value       = "5432"
    },
    {
      name_suffix = "name"
      value       = local.db_name
    }
  ]

}

resource "azurerm_key_vault_secret" "flexible_secret" {
  for_each     = { for secret in local.flexible_secrets : secret.name_suffix => secret }
  key_vault_id = data.azurerm_key_vault.reform_scan_key_vault.id
  name         = "${local.flexible_secret_prefix}-${each.value.name_suffix}"
  value        = each.value.value
  tags = merge(var.common_tags, {
    "source" : "${var.component} PostgreSQL"
  })
  content_type    = ""
  expiration_date = timeadd(timestamp(), "17520h")
}

resource "azurerm_key_vault_secret" "flexible_secret_staging" {
  for_each     = { for secret in local.flexible_secrets_staging : secret.name_suffix => secret }
  key_vault_id = data.azurerm_key_vault.reform_scan_key_vault.id
  name         = "${local.flexible_secret_prefix_staging}-${each.value.name_suffix}"
  value        = each.value.value
  tags = merge(var.common_tags, {
    "source" : "${var.component} PostgreSQL"
  })
  content_type    = ""
  expiration_date = timeadd(timestamp(), "17520h")
}
