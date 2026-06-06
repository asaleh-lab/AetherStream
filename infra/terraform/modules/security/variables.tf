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

variable "tenant_id" {
  type = string
}

variable "terraform_principal_id" {
  description = "Object ID of the identity running Terraform (for Key Vault access during apply)."
  type        = string
}
