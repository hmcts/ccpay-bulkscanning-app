variable "product" {
  type    = "string"
  default = "payment"
}

variable "component" {
  type    = "string"
  default = "bs-payment-api"

}

variable "location_app" {
  type    = "string"
  default = "UK South"
}

variable "env" {
  type = "string"
}

variable "ilbIp" {}

variable "subscription" {}

variable "capacity" {
  default = "1"
}

variable "common_tags" {
  type = "map"
}
