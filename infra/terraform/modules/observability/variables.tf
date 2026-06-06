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

variable "log_analytics_workspace_id" {
  type = string
}

variable "app_service_ids" {
  type    = list(string)
  default = []
}

variable "aks_id" {
  type    = string
  default = ""
}
