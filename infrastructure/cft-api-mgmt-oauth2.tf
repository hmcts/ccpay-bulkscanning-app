# Note for API docs see - https://github.com/hmcts/cnp-api-docs/tree/master/docs/specs

locals {
  cft_api_mgmt_oauth2_suffix = var.apim_suffix == "" ? var.env : var.apim_suffix
  cft_api_mgmt_oauth2_name   = join("-", ["cft-api-mgmt", local.cft_api_mgmt_suffix])
  cft_api_mgmt_oauth2_rg     = join("-", ["cft", var.env, "network-rg"])
  cft_api_oauth2_base_path   = "payments-bulk-scanning-api"
}

data "template_file" "cft_oauth2_policy_template" {
  template = file("${path.module}/template/cft-api-policy-oauth2.xml")

  vars = {
    cft_oauth2_tenant_id = data.azurerm_key_vault_secret.tenant_id.value
    cft_oauth2_client_id = data.azurerm_key_vault_secret.apim_client_id.value
    cft_oauth2_app_id    = data.azurerm_key_vault_secret.apim_app_id.value
    s2s_client_id        = data.azurerm_key_vault_secret.s2s_client_id.value
    s2s_base_url         = local.s2sUrl
  }

  depends_on = [
    resource.azurerm_api_management_named_value.ccpay_s2s_client_id
  ]
}

resource "azurerm_api_management_named_value" "ccpay_s2s_client_secret" {
  name                = "ccpay-s2s-client-secret"
  resource_group_name = local.cft_api_mgmt_oauth2_rg
  api_management_name = local.cft_api_mgmt_oauth2_name
  display_name        = "ccpay-s2s-client-secret"
  value               = data.azurerm_key_vault_secret.s2s_client_secret.value
  secret              = true
  provider            = azurerm.aks-cftapps

  depends_on = [
    module.cft_api_mgmt_oauth2_api
  ]
}

module "cft_api_mgmt_oauth2_product" {
  source                        = "git@github.com:hmcts/cnp-module-api-mgmt-product?ref=master"
  name                          = "payments-bulk-scanning"
  api_mgmt_name                 = local.cft_api_mgmt_oauth2_name
  api_mgmt_rg                   = local.cft_api_mgmt_oauth2_rg
  approval_required             = "false"
  subscription_required         = "false"
  product_access_control_groups = ["developers"]
  providers = {
    azurerm = azurerm.aks-cftapps
  }
}

module "cft_api_mgmt_oauth2_api" {
  source                = "git@github.com:hmcts/cnp-module-api-mgmt-api?ref=master"
  name                  = "payments-bulk-scanning-api"
  display_name          = "Bulk Scanning Payments API"
  api_mgmt_name         = local.cft_api_mgmt_oauth2_name
  api_mgmt_rg           = local.cft_api_mgmt_oauth2_rg
  product_id            = module.cft_api_mgmt_oauth2_product.product_id
  path                  = local.cft_api_oauth2_base_path
  service_url           = "http://ccpay-bulkscanning-api-${var.env}.service.core-compute-${var.env}.internal"
  swagger_url           = "https://raw.githubusercontent.com/hmcts/cnp-api-docs/master/docs/specs/ccpay-payment-app.bulk-scanning.json"
  protocols             = ["http", "https"]
  content_format        = "openapi-link"
  subscription_required = "false"
  revision              = "2"
  providers = {
    azurerm = azurerm.aks-cftapps
  }
}



module "cft_api_mgmt_oauth2_policy" {
  source                 = "git@github.com:hmcts/cnp-module-api-mgmt-api-policy?ref=master"
  api_mgmt_name          = local.cft_api_mgmt_oauth2_name
  api_mgmt_rg            = local.cft_api_mgmt_oauth2_rg
  api_name               = module.cft_api_mgmt_oauth2_api.name
  api_policy_xml_content = data.template_file.cft_oauth2_policy_template.rendered
  providers = {
    azurerm = azurerm.aks-cftapps
  }

  depends_on = [
    module.cft_api_mgmt_oauth2_api
  ]
}
