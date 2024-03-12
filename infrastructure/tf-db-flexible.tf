module "postgresql" {
  providers = {
    azurerm.postgres_network = azurerm.postgres_network
  }

  source               = "git@github.com:hmcts/terraform-module-postgresql-flexible?ref=master"
  name                 = local.db_host_name
  product              = var.product
  component            = var.component
  location             = var.location
  env                  = var.env
  pgsql_admin_username = local.postgresql_user
  pgsql_databases = [
    {
      name : local.db_name
    }
  ]
  subnet_suffix = "expanded"
  common_tags   = var.common_tags
  business_area = "cft"
  pgsql_version = "15"

  admin_user_object_id = var.jenkins_AAD_objectId
  force_user_permissions_trigger = "1"
}

module "postgresql_staging" {
  count = var.env == "aat" ? 1 : 0
  providers = {
    azurerm.postgres_network = azurerm.postgres_network
  }

  source               = "git@github.com:hmcts/terraform-module-postgresql-flexible?ref=master"
  name                 = "${local.db_host_name}-staging"
  product              = "${var.component}-staging"
  component            = var.component
  location             = var.location
  env                  = "aat"
  pgsql_admin_username = "blob_router"
  pgsql_databases = [
    {
      name : local.db_name
    }
  ]
  subnet_suffix = "expanded"
  common_tags   = var.common_tags
  business_area = "cft"
  pgsql_version = "15"

  admin_user_object_id = var.jenkins_AAD_objectId
}
