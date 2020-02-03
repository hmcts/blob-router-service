variable "product" {}

variable "raw_product" {
  default = "blob-router" // jenkins-library overrides product for PRs and adds e.g. pr-118-bulk-scan
}

variable "component" {}

variable "env" {
  type = "string"
}

variable "subscription" {
  type = "string"
}

variable "common_tags" {
  type = "map"
}

# list of SSL client certificate thumbprints that are accepted by the API (gateway)
# (excludes certificates used by API tests)
variable "allowed_client_certificate_thumbprints" {
  type    = "list"
  default = []
}

variable "location_db" {
  type    = "string"
  default = "UK South"
}

variable "deployment_namespace" {
  default = ""
}
