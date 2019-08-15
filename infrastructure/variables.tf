variable "product" {
  type    = "string"
  default = "ccpay"
}

variable "component" {
  type    = "string"
  default = "bulkscanning-api"

}

variable "location_app" {
  type    = "string"
  default = "UK South"
}

variable "env" {
  type = "string"
}

variable "ilbIp" {}

variable "subscription" {
  type    = "string"
}

variable "capacity" {
  default = "1"
}

variable "common_tags" {
  type = "map"
}

variable "database_name" {
  type    = "string"
  default = "ccpay-bulkscanning-payment"
}

variable "postgresql_user" {
  type    = "string"
  default = "ccpay-bulkscanning-payment"
}
