provider "azurerm" {
  version = "1.22.1"
}

locals {

  asp_name = "ccpay-${var.env}"
  sku_size = "${var.env == "prod" || var.env == "sprod" || var.env == "aat" ? "I2" : "I1"}"

  previewVaultName = "ccpay-aat"
  nonPreviewVaultName = "ccpay-${var.env}"
  vaultName = "${(var.env == "preview" || var.env == "spreview") ? local.previewVaultName : local.nonPreviewVaultName}"

  #region API gateway
  thumbprints_in_quotes = "${formatlist("&quot;%s&quot;", var.bulkscanning_api_gateway_certificate_thumbprints)}"
  thumbprints_in_quotes_str = "${join(",", local.thumbprints_in_quotes)}"

  api_policy = "${replace(file("template/api-policy.xml"), "ALLOWED_CERTIFICATE_THUMBPRINTS", local.thumbprints_in_quotes_str)}"
  api_base_path = "bulk-scanning-payment"
}

module "bulk-scanning-payment-api" {
  source = "git@github.com:hmcts/cnp-module-webapp?ref=master"
  product = "${var.product}-${var.component}"
  location = "${var.location_app}"
  env = "${var.env}"
  ilbIp = "${var.ilbIp}"
  subscription = "${var.subscription}"
  capacity = "${var.capacity}"
  common_tags = "${var.common_tags}"
  appinsights_instrumentation_key = "${data.azurerm_key_vault_secret.appinsights_instrumentation_key.value}"
  appinsights_instrumentation_key = "${data.azurerm_key_vault_secret.appinsights_instrumentation_key.value}"

  asp_name = "${local.asp_name}"
  asp_rg = "${local.asp_name}"
  instance_size = "${local.sku_size}"

  app_settings = {
    TEST="true"
    LOGBACK_REQUIRE_ALERT_LEVEL = "false"
    LOGBACK_REQUIRE_ERROR_CODE = "false"
    SPRING_DATASOURCE_USERNAME = "${module.ccpay-bulkscanning-payment-database.user_name}"
    SPRING_DATASOURCE_PASSWORD = "${module.ccpay-bulkscanning-payment-database.postgresql_password}"
    SPRING_DATASOURCE_URL = "jdbc:postgresql://${module.ccpay-bulkscanning-payment-database.host_name}:${module.ccpay-bulkscanning-payment-database.postgresql_listen_port}/${module.ccpay-bulkscanning-payment-database.postgresql_database}?sslmode=require"
  }
}
module "ccpay-bulkscanning-payment-database" {
  source = "git@github.com:hmcts/cnp-module-postgres?ref=master"
  product = "${var.product}-${var.component}-postgres-db"
  location = "${var.location_app}"
  subscription = "${var.subscription}"
  env = "${var.env}"
  postgresql_user = "${var.postgresql_user}"
  database_name = "${var.database_name}"
  sku_name = "GP_Gen5_2"
  sku_tier = "GeneralPurpose"
  common_tags = "${var.common_tags}"
}

# Populate Vault with DB info

resource "azurerm_key_vault_secret" "POSTGRES-USER" {
  name      = "${var.component}-POSTGRES-USER"
  value     = "${module.ccpay-bulkscanning-payment-database.user_name}"
  key_vault_id = "${data.azurerm_key_vault.payment_key_vault.id}"
}

resource "azurerm_key_vault_secret" "POSTGRES-PASS" {
  name      = "${var.component}-POSTGRES-PASS"
  value     = "${module.ccpay-bulkscanning-payment-database.postgresql_password}"
  key_vault_id = "${data.azurerm_key_vault.payment_key_vault.id}"
}

resource "azurerm_key_vault_secret" "POSTGRES_HOST" {
  name      = "${var.component}-POSTGRES-HOST"
  value     = "${module.ccpay-bulkscanning-payment-database.host_name}"
  key_vault_id = "${data.azurerm_key_vault.payment_key_vault.id}"
}

resource "azurerm_key_vault_secret" "POSTGRES_PORT" {
  name      = "${var.component}-POSTGRES-PORT"
  value     = "${module.ccpay-bulkscanning-payment-database.postgresql_listen_port}"
  key_vault_id = "${data.azurerm_key_vault.payment_key_vault.id}"
}

resource "azurerm_key_vault_secret" "POSTGRES_DATABASE" {
  name      = "${var.component}-POSTGRES-DATABASE"
  value     = "${module.ccpay-bulkscanning-payment-database.postgresql_database}"
  key_vault_id = "${data.azurerm_key_vault.payment_key_vault.id}"
}


data "azurerm_key_vault" "payment_key_vault" {
  name = "${local.vaultName}"
  resource_group_name = "ccpay-${var.env}"
}
data "azurerm_key_vault_secret" "appinsights_instrumentation_key" {
  name = "AppInsightsInstrumentationKey"
  vault_uri = "${data.azurerm_key_vault.payment_key_vault.vault_uri}"
}

data "template_file" "policy_template" {
  template = "${file("${path.module}/template/api-policy.xml")}"

  vars {
    allowed_certificate_thumbprints = "${local.thumbprints_in_quotes_str}"
  }
}

data "template_file" "api_template" {
  template = "${file("${path.module}/template/api.json")}"
}

resource "azurerm_template_deployment" "bulk-scanning-payment" {
  template_body       = "${data.template_file.api_template.rendered}"
  name                = "bulk-scanning-payment-${var.env}"
  deployment_mode     = "Incremental"
  resource_group_name = "core-infra-${var.env}"
  count               = "${var.env != "preview" ? 1: 0}"

  parameters = {
    apiManagementServiceName  = "core-api-mgmt-${var.env}"
    apiName                   = "bulk-scanning-payment-api"
    apiProductName            = "bulk-scanning-payment"
    serviceUrl                = "http://ccpay-bulkscanning-api-${var.env}.service.core-compute-${var.env}.internal"
    apiBasePath               = "${local.api_base_path}"
    policy                    = "${data.template_file.policy_template.rendered}"
  }
}

