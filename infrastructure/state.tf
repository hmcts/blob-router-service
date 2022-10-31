terraform {
  backend "azurerm" {}

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "=3.29.1"
    }
    azuread = {
      source  = "hashicorp/azuread"
      version = "2.30.0"
    }
    random = {
      source = "hashicorp/random"
    }
  }
}
