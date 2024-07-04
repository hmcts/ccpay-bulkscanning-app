locals {
  cft_api_mgmt_suffix = var.apim_suffix == "" ? var.env : var.apim_suffix
  cft_api_mgmt_name   = join("-", ["cft-api-mgmt", local.cft_api_mgmt_suffix])
  cft_api_mgmt_rg     = join("-", ["cft", var.env, "network-rg"])
}

module "cft_api_mgmt_product" {
  source        = "git@github.com:hmcts/cnp-module-api-mgmt-product?ref=master"
  name          = var.product_name
  api_mgmt_name = local.cft_api_mgmt_name
  api_mgmt_rg   = local.cft_api_mgmt_rg
  providers = {
    azurerm = azurerm.aks-cftapps
  }
}

resource "azurerm_resource_group_template_deployment" "cft-bulk-scanning-payment" {
  template_content    = data.template_file.api_template.rendered
  name                = "cft-bulk-scanning-payment-${var.env}"
  deployment_mode     = "Incremental"
  resource_group_name = local.cft_api_mgmt_rg
  count               = var.env != "preview" ? 1 : 0

  parameters_content = jsonencode({
    apiManagementServiceName = { value = local.cft_api_mgmt_rg }
    apiName                  = { value = "bulk-scanning-payment-api" }
    apiProductName           = { value = "bulk-scanning-payment" }
    serviceUrl               = { value = "http://ccpay-bulkscanning-api-${var.env}.service.core-compute-${var.env}.internal" }
    apiBasePath              = { value = local.api_base_path }
    policy                   = { value = data.template_file.policy_template.rendered }
  })
}
