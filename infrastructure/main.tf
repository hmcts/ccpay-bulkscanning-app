provider "azurerm" {
  version = "=2.20.0"
  features {}
}

locals {
  aseName = "core-compute-${var.env}"

  local_env = "${(var.env == "preview" || var.env == "spreview") ? (var.env == "preview" ) ? "aat" : "saat" : var.env}"
  local_ase = "${(var.env == "preview" || var.env == "spreview") ? (var.env == "preview" ) ? "core-compute-aat" : "core-compute-saat" : local.aseName}"

  previewVaultName = "ccpay-aat"
  nonPreviewVaultName = "ccpay-${var.env}"
  vaultName = "${(var.env == "preview" || var.env == "spreview") ? local.previewVaultName : local.nonPreviewVaultName}"
  s2sUrl = "http://rpe-service-auth-provider-${local.local_env}.service.${local.local_ase}.internal"

  #region API gateway
  thumbprints_in_quotes = "${formatlist("&quot;%s&quot;", var.bulkscanning_api_gateway_certificate_thumbprints)}"
  thumbprints_in_quotes_str = "${join(",", local.thumbprints_in_quotes)}"

  api_policy = "${replace(file("template/api-policy.xml"), "ALLOWED_CERTIFICATE_THUMBPRINTS", local.thumbprints_in_quotes_str)}"
  api_base_path = "bulk-scanning-payment"
}

module "ccpay-bulkscanning-payment-database" {
  source = "git@github.com:hmcts/cnp-module-postgres?ref=master"
  product = "${var.product}-${var.component}-postgres-db"
  location = var.location_app
  subscription = var.subscription
  env = var.env
  postgresql_user = var.postgresql_user
  database_name = var.database_name
  sku_name = "GP_Gen5_2"
  sku_tier = "GeneralPurpose"
  common_tags = var.common_tags
}

data "azurerm_key_vault" "payment_key_vault" {
  name = local.vaultName
  resource_group_name = "ccpay-${var.env}"
}

# Populate Vault with DB info

resource "azurerm_key_vault_secret" "POSTGRES-USER" {
  name      = join("-", [var.component, "POSTGRES-USER"])
  value     = module.ccpay-bulkscanning-payment-database.user_name
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES-PASS" {
  name      = join("-", [var.component, "POSTGRES-PASS"])
  value     = module.ccpay-bulkscanning-payment-database.postgresql_password
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_HOST" {
  name      = join("-", [var.component, "POSTGRES-HOST"])
  value     =  module.ccpay-bulkscanning-payment-database.host_name
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_PORT" {
  name      = join("-", [var.component, "POSTGRES-PORT"])
  value     =  module.ccpay-bulkscanning-payment-database.postgresql_listen_port
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_DATABASE" {
  name      = join("-", [var.component, "POSTGRES-DATABASE"])
  value     =  module.ccpay-bulkscanning-payment-database.postgresql_database
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}


# region API (gateway)
data "azurerm_key_vault_secret" "s2s_client_secret" {
  name = "gateway-s2s-client-secret"
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}

data "azurerm_key_vault_secret" "s2s_client_id" {
  name = "gateway-s2s-client-id"
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}

data "template_file" "api_template" {
  template = "${file("${path.module}/template/api.json")}"
}

data "template_file" "policy_template" {
  template = "${file("${path.module}/template/api-policy.xml")}"

  vars = {
    allowed_certificate_thumbprints = "${local.thumbprints_in_quotes_str}"
    s2s_client_id = "${data.azurerm_key_vault_secret.s2s_client_id.value}"
    s2s_client_secret = "${data.azurerm_key_vault_secret.s2s_client_secret.value}"
    s2s_base_url = "${local.s2sUrl}"
  }
}

resource "azurerm_template_deployment" "bulk-scanning-payment" {
  template_body       = data.template_file.api_template.rendered
  name                = "bulk-scanning-payment-${var.env}"
  deployment_mode     = "Incremental"
  resource_group_name = "core-infra-${var.env}"
  count               = var.env != "preview" ? 1: 0

  parameters = {
    apiManagementServiceName  = "core-api-mgmt-${var.env}"
    apiName                   = "bulk-scanning-payment-api"
    apiProductName            = "bulk-scanning-payment"
    serviceUrl                = "http://ccpay-bulkscanning-api-${var.env}.service.core-compute-${var.env}.internal"
    apiBasePath               = local.api_base_path
    policy                    = data.template_file.policy_template.rendered
  }
}
