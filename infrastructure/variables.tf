variable "product" {
  default = ccpay
}

variable "component" {
  default = "bulkscanning-api"

}

variable "location_app" {
  default = "UK South"
}

variable "env" {
}

variable "subscription" {
}

variable "common_tags" {
  type = map(string)
}

variable "database_name" {
  default = "bspayment"
}

variable "postgresql_user" {
  default = "bspayment"
}

# thumbprint of the SSL certificate for API gateway tests
variable bulkscanning_api_gateway_certificate_thumbprints {
  type = "list"
  default = []
}
