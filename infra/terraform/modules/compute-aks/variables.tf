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

variable "aks_subnet_id" {
  type = string
}

variable "aks_node_count" {
  type    = number
  default = 1
}

variable "aks_vm_size" {
  type    = string
  default = "Standard_B2als_v2"
}

variable "aks_kubernetes_version" {
  type    = string
  default = null
}

variable "acr_id" {
  type = string
}
