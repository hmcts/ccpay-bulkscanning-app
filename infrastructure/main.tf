provider "azurerm" {
  version = "1.22.1"
}

locals {

  asp_name = "ccpay-${var.env}"
  sku_size = "${var.env == "prod" || var.env == "sprod" || var.env == "aat" ? "I2" : "I1"}"

  previewVaultName = "ccpay-aat"
  nonPreviewVaultName = "ccpay-${var.env}"
  vaultName = "${(var.env == "preview" || var.env == "spreview") ? local.previewVaultName : local.nonPreviewVaultName}"

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
    POSTGRES_USERNAME = "${module.ccpay-bulkscanning-payment-database.user_name}"
    POSTGRES_PASSWORD = "${module.ccpay-bulkscanning-payment-database.postgresql_password}"
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
