provider "azurerm" {
  version = "1.22.1"
}

locals {
  ase_name = "${data.terraform_remote_state.core_apps_compute.ase_name[0]}"
  asp_name = "ccpay-${var.env}"}"
  sku_size = "${var.env == "prod" || var.env == "sprod" || var.env == "aat" ? "I2" : "I1"}"

  previewVaultName = "ccpay-aat"
  nonPreviewVaultName = "ccpay-${var.env}"
  vaultName = "${(var.env == "preview" || var.env == "spreview") ? local.previewVaultName : local.nonPreviewVaultName}"

}

module "bulk-scanning-payment-api" {
  source              = "git@github.com:hmcts/cnp-module-webapp?ref=master"
  product             = "${var.product}-${var.component}"
  location            = "${var.location_app}"
  env                 = "${var.env}"
  ilbIp               = "${var.ilbIp}"
  subscription        = "${var.subscription}"
  capacity            = "${var.capacity}"
  common_tags         = "${var.common_tags}"
  appinsights_instrumentation_key = "${data.azurerm_key_vault_secret.appinsights_instrumentation_key.value}"
  appinsights_instrumentation_key = "${data.azurerm_key_vault_secret.appinsights_instrumentation_key.value}"

asp_name = "${local.asp_name}"
  asp_rg = "${local.asp_name}"
  instance_size = "${local.sku_size}"

  app_settings = {
    LOGBACK_REQUIRE_ALERT_LEVEL = "false"
    LOGBACK_REQUIRE_ERROR_CODE  = "false"
  }
}
data "azurerm_key_vault" "payment_key_vault" {
name = "${local.vaultName}"
resource_group_name = "ccpay-${local.local_env}"
}
data "azurerm_key_vault_secret" "appinsights_instrumentation_key" {
  name = "AppInsightsInstrumentationKey"
  vault_uri = "${data.azurerm_key_vault.payment_key_vault.vault_uri}"
}
