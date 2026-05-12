provider "azurerm" {
  features {
    resource_group {
      prevent_deletion_if_contains_resources = false
    }
  }
}

locals {
  aseName = "core-compute-${var.env}"

  local_env = (var.env == "preview" || var.env == "spreview") ? (var.env == "preview") ? "aat" : "saat" : var.env
  local_ase = (var.env == "preview" || var.env == "spreview") ? (var.env == "preview") ? "core-compute-aat" : "core-compute-saat" : local.aseName

  previewVaultName    = "ccpay-aat"
  nonPreviewVaultName = "ccpay-${var.env}"
  vaultName           = (var.env == "preview" || var.env == "spreview") ? local.previewVaultName : local.nonPreviewVaultName
  s2sUrl              = "http://rpe-service-auth-provider-${local.local_env}.service.${local.local_ase}.internal"

  #region API gateway
  thumbprints_in_quotes     = formatlist("&quot;%s&quot;", var.bulkscanning_api_gateway_certificate_thumbprints)
  thumbprints_in_quotes_str = join(",", local.thumbprints_in_quotes)
  api_base_path             = "bulk-scanning-payment"
  db_server_name            = "${var.product}-${var.component}-postgres-db-v15"
}

data "azurerm_key_vault" "payment_key_vault" {
  name                = local.vaultName
  resource_group_name = "ccpay-${var.env}"
}

module "ccpay-bulkscanning-payment-database-v15" {
  providers = {
    azurerm.postgres_network = azurerm.postgres_network
  }
  source               = "git@github.com:hmcts/terraform-module-postgresql-flexible?ref=master"
  product              = var.product
  component            = var.component
  business_area        = "cft"
  name                 = local.db_server_name
  location             = var.location_app
  env                  = var.env
  pgsql_admin_username = var.postgresql_user

  # Setup Access Reader db user
  force_user_permissions_trigger = "1"

  pgsql_databases = [
    {
      name : var.database_name
    }
  ]
  pgsql_server_configuration = [
    {
      name  = "azure.extensions"
      value = "pg_stat_statements,pg_buffercache"
    }
  ]
  admin_user_object_id       = var.jenkins_AAD_objectId
  common_tags                = var.common_tags
  pgsql_version              = var.postgresql_flexible_sql_version
  action_group_name          = join("-", [var.db_monitor_action_group_name, local.db_server_name, var.env])
  email_address_key          = var.db_alert_email_address_key
  email_address_key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}

# Populate Vault with DB info

resource "azurerm_key_vault_secret" "POSTGRES-USER" {
  name         = join("-", [var.component, "POSTGRES-USER"])
  value        = module.ccpay-bulkscanning-payment-database-v15.username
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES-PASS" {
  name         = join("-", [var.component, "POSTGRES-PASS"])
  value        = module.ccpay-bulkscanning-payment-database-v15.password
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_HOST" {
  name         = join("-", [var.component, "POSTGRES-HOST"])
  value        = module.ccpay-bulkscanning-payment-database-v15.fqdn
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_PORT" {
  name         = join("-", [var.component, "POSTGRES-PORT"])
  value        = var.postgresql_flexible_server_port
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_DATABASE" {
  name         = join("-", [var.component, "POSTGRES-DATABASE"])
  value        = var.database_name
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}

# region API (gateway)
data "azurerm_key_vault_secret" "s2s_client_secret" {
  name         = "gateway-s2s-client-secret"
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}

data "azurerm_key_vault_secret" "s2s_client_id" {
  name         = "gateway-s2s-client-id"
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}

data "azurerm_key_vault_secret" "apim_app_id" {
  name         = "apim-bulk-scanning-app-id"
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}

data "azurerm_key_vault_secret" "apim_client_id" {
  name         = "apim-bulk-scanning-client-id"
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}

data "azurerm_key_vault_secret" "tenant_id" {
  name         = "apim-bulk-scanning-tenant-id"
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}
