variable "prefix" {
  type = string
}

variable "location" {
  type = string
}

variable "resource_group_name" {
  type = string
}

variable "tags" {
  type = map(string)
}

variable "address_space" {
  type    = list(string)
  default = ["10.1.0.0/16"]
}

variable "aks_subnet_prefix" {
  type    = string
  default = "10.1.0.0/20"
}
