terraform {
  backend "azurerm" {}

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "3.106.1"
    }
    azuread = {
      source  = "hashicorp/azuread"
      version = "2.48.0"
    }
    random = {
      source = "hashicorp/random"
    }
  }
}
