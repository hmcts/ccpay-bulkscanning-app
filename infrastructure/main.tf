provider "azurerm" {
  version = "1.22.1"
}

locals {

  asp_name = "ccpay-${var.env}"
  sku_size = "${var.env == "prod" || var.env == "sprod" || var.env == "aat" ? "I2" : "I1"}"
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

    # S2S trusted services
    TRUSTED_S2S_SERVICE_NAMES = "api_gw, ccpay_bubble"

    # idam
    AUTH_IDAM_CLIENT_BASEURL = "${var.idam_api_url}"

    # service-auth-provider
    AUTH_PROVIDER_SERVICE_CLIENT_BASEURL = "${local.s2sUrl}"
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

# region API (gateway)
data "azurerm_key_vault_secret" "s2s_client_secret" {
  name = "gateway-s2s-client-secret"
  vault_uri = "${data.azurerm_key_vault.payment_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "s2s_client_id" {
  name = "gateway-s2s-client-id"
  vault_uri = "${data.azurerm_key_vault.payment_key_vault.vault_uri}"
}

data "template_file" "policy_template" {
  template = "${file("${path.module}/template/api-policy.xml")}"

  vars {
    allowed_certificate_thumbprints = "${local.thumbprints_in_quotes_str}"
    s2s_client_id = "${data.azurerm_key_vault_secret.s2s_client_id.value}"
    s2s_client_secret = "${data.azurerm_key_vault_secret.s2s_client_secret.value}"
    s2s_base_url = "${local.s2sUrl}"
  }
}

data "template_file" "api_template" {
  template = "${file("${path.module}/template/bulk-scanning-payment.json")}"
}


resource "azurerm_api_management_group" "bulk-scanning-payments-api-group" {
  name                = "bulk-scanning-payment2"
  resource_group_name = "core-infra-${var.env}"
  api_management_name = "core-api-mgmt-${var.env}"
  display_name        = "Developers"
  description         = "Developers group"
}

resource "azurerm_api_management_product" "bulk-scanning-payment-product" {
  product_id            = "bulk-scanning-payment-api2"
  api_management_name   = "core-api-mgmt-${var.env}"
  resource_group_name   = "core-infra-${var.env}"
  display_name          = "bulk-scanning-payment-api-product2"
  subscription_required = false
  approval_required     = false
  published             = true
}

resource "azurerm_api_management_api_policy" "bulk-scanning-payment-policy" {
  api_name            = "${azurerm_api_management_api.bulk-scanning-payment2.name}"
  api_management_name = "core-api-mgmt-${var.env}"
  resource_group_name = "core-infra-${var.env}"
  xml_content = "${data.template_file.policy_template.rendered}"
}

resource "azurerm_api_management_api_operation" "bulk-scanning-payment-operation" {
  operation_id        = "bulkscanning-payment-post2"
  api_name            = "${azurerm_api_management_api.bulk-scanning-payment2.name}"
  api_management_name = "core-api-mgmt-${var.env}"
  resource_group_name = "core-infra-${var.env}"
  display_name        = "Post Bulkscanning payment Operation"
  method              = "POST"
  url_template        = "/bulk-scan-payment"
  description         = "Used by third party scanning service to post payments data into Bulk-scanning service"

  request {
    representation {
      content_type = "application/json"
      type_name = "object"
      sample = {
        "document_control_number": "XXYYYYZZZZ",
        "amount": "550.00",
        "currency": "GBP",
        "method": "Cheque",
        "bank_giro_credit_slip_number": 123456,
        "banked_date": "2018-01-01"
      }
    }
  }
  response {
    description = "Created"
    status_code = 200
  }

}

resource "azurerm_api_management_api" "bulk-scanning-payment2" {
  name                = "bulk-scanning-payment-${var.env}2"
  resource_group_name = "core-infra-${var.env}"
  display_name        = "bulk-scanning payments API2"
  revision            = 1
  api_management_name = "core-api-mgmt-${var.env}"
  protocols           = ["https"]
  service_url         = "http://ccpay-bulkscanning-api-${var.env}.service.core-compute-${var.env}.internal"
  path                =  "${local.api_base_path}"
  import {
    content_format = "swagger-json"
    content_value = "${data.template_file.api_template.rendered}"
  }

}

