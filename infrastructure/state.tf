terraform {
  backend "azurerm" {}

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "=3.24.0"
    }
    azuread = {
      source  = "hashicorp/azuread"
      version = "2.28.1"
    }
    random = {
      source = "hashicorp/random"
    }
  }
}
