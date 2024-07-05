provider "azurerm" {
  features {}
  skip_provider_registration = true
  alias                      = "postgres_network"
  subscription_id            = var.aks_subscription_id
}

provider "azurerm" {
  features {
      resource_group {
        prevent_deletion_if_contains_resources = false
      }
  }
  alias           = "aks-cftapps"
  subscription_id = var.aks_subscription_id
}
