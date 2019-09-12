variable "product" {
  type    = "string"
  default = "ccpay"
}

variable "component" {
  type    = "string"
  default = "bulkscanning-api"
}

variable "location" {
  type    = "string"
  default = "UK South"
}

variable "env" {
  type = "string"
}

variable "ilbIp" {}

variable "subscription" {
  type = "string"
}

variable "capacity" {
  default = "1"
}

variable "common_tags" {
  type = "map"
}

variable "database_name" {
  type    = "string"
  default = "bspayment"
}

variable "postgresql_user" {
  type    = "string"
  default = "bspayment"
}

variable "enable_ase" {
  default = true
}

variable "appinsights_location" {
  type        = "string"
  default     = "West Europe"
  description = "Location for Application Insights"
}
