terraform {
  backend "azurerm" {}

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "=3.25.0"
    }
    azuread = {
      source  = "hashicorp/azuread"
      version = "2.29.0"
    }
    random = {
      source = "hashicorp/random"
    }
  }
}
