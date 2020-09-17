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
