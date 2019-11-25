# thumbprint of the valid SSL certificate for API gateway tests
variable api_test_valid_certificate_thumbprint {
  type = "string"

  # keeping this empty by default, so that no thumbprint will match
  default = ""
}

# thumbprint of the test certificate that's expired (used by API gateway tests)
variable api_test_expired_certificate_thumbprint {
  type    = "string"
  default = "will be added with tests"
}

# thumbprint of the test certificate that's not yet valid (used by API gateway tests)
variable api_test_not_yet_valid_certificate_thumbprint {
  type = "string"

  # valid since year 2100
  default = "will be added with tests"
}
