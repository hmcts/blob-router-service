variable "product" {}

variable "raw_product" {
  default = "blob-router" // jenkins-library overrides product for PRs and adds e.g. pr-118-bulk-scan
}

variable "component" {}

variable "env" {}

variable "subscription" {}

variable "common_tags" {
  type = map(string)
}

variable "aks_subscription_id" {
  default = ""
}

variable "jenkins_AAD_objectId" {
  type        = string
  description = "(Required) The Azure AD object ID of a user, service principal or security group in the Azure Active Directory tenant for the vault. The object ID must be unique for the list of access policies."
}

variable "location" {
  default = "UK South"
}

# list of SSL client certificate thumbprints that are accepted by the API (gateway)
# (excludes certificates used by API tests)
variable "allowed_client_certificate_thumbprints" {
  type    = list(string)
  default = []
}

variable "location_db" {
  default = "UK South"
}

variable "deployment_namespace" {
  default = ""
}

variable "num_staging_dbs" {
  default = 0
}

variable "schema_ownership_trigger" {
  default = "true"
}

variable "apim_suffix" {
  default = ""
}