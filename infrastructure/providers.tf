terraform {
  backend "azurerm" {}

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "4.39.0"
    }
    azuread = {
      source  = "hashicorp/azuread"
      version = "3.5.0"
    }
    random = {
      source = "hashicorp/random"
    }
  }
}

provider "azurerm" {
  features {}
}

provider "azurerm" {
  features {}
  alias           = "postgres_network"
  subscription_id = var.aks_subscription_id
}

provider "azurerm" {
  alias           = "aks-cftapps"
  subscription_id = var.cft_subscription_id
  features {}
}
