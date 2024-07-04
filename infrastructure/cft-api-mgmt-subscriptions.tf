# Subscription keys for the CFT APIM

# Internal subscription - Bulk Scan DTS Team
resource "azurerm_api_management_subscription" "fee_pay_team_bulk_scan_subscription" {
  api_management_name = local.cft_api_mgmt_name
  resource_group_name = local.cft_api_mgmt_rg
  product_id          = module.cft_api_mgmt_product.id
  display_name        = "Bulk Scanning Payment API - Fee and Pay DTS Team Subscription"
  state               = "active"
  provider            = azurerm.aks-cftapps
}

resource "azurerm_key_vault_secret" "fee_pay_team_bulk_scan_subscription_key" {
  name         = "fee-pay-team-bulk-scan-cft-apim-subscription-key"
  value        = azurerm_api_management_subscription.fee_pay_team_bulk_scan_subscription.primary_key
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}

# Supplier subscription - Exela
resource "azurerm_api_management_subscription" "exela_supplier_subscription" {
  api_management_name = local.cft_api_mgmt_name
  resource_group_name = local.cft_api_mgmt_rg
  product_id          = module.cft_api_mgmt_product.id
  display_name        = "Bulk Scanning Payment API - Exela Supplier Subscription"
  state               = "active"
  provider            = azurerm.aks-cftapps
}

resource "azurerm_key_vault_secret" "exela_supplier_subscription_key" {
  name         = "exela-cft-apim-subscription-key"
  value        = azurerm_api_management_subscription.exela_supplier_subscription.primary_key
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}

# Supplier subscription - Iron Mountain
resource "azurerm_api_management_subscription" "iron_mountain_supplier_subscription" {
  api_management_name = local.cft_api_mgmt_name
  resource_group_name = local.cft_api_mgmt_rg
  product_id          = module.cft_api_mgmt_product.id
  display_name        = "Bulk Scanning Payment API - Iron Mountain Supplier Subscription"
  state               = "active"
  provider            = azurerm.aks-cftapps
}

resource "azurerm_key_vault_secret" "iron_mountain_supplier_subscription_key" {
  name         = "iron-mountain-cft-apim-subscription-key"
  value        = azurerm_api_management_subscription.iron_mountain_supplier_subscription.primary_key
  key_vault_id = data.azurerm_key_vault.payment_key_vault.id
}
