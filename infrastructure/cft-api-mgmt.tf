locals {
  cft_api_mgmt_name     = join("-", ["cft-api-mgmt", var.env])
  cft_api_mgmt_rg       = join("-", ["cft", var.env, "network-rg"])
}

resource "azurerm_template_deployment" "cft-bulk-scanning-payment" {
  template_body       = data.template_file.api_template.rendered
  name                = "cft-bulk-scanning-payment-${var.env}"
  deployment_mode     = "Incremental"
  resource_group_name = local.cft_api_mgmt_rg
  count               = var.env != "preview" ? 1: 0

  parameters = {
    apiManagementServiceName  = local.cft_api_mgmt_name
    apiName                   = "bulk-scanning-payment-api"
    apiProductName            = "bulk-scanning-payment"
    serviceUrl                = "http://ccpay-bulkscanning-api-${var.env}.service.core-compute-${var.env}.internal"
    apiBasePath               = local.api_base_path
    policy                    = data.template_file.policy_template.rendered
  }
}
