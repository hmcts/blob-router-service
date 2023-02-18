terraform {
  backend "azurerm" {}

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "3.44.1"
    }
    azuread = {
      source  = "hashicorp/azuread"
      version = "2.34.1"
    }
    random = {
      source = "hashicorp/random"
    }
  }
}
