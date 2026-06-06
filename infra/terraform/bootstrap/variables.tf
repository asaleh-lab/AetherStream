variable "location" {
  description = "Azure region for bootstrap resources."
  type        = string
  default     = "westeurope"
}

variable "prefix" {
  description = "Naming prefix for bootstrap resources."
  type        = string
  default     = "aether"
}

variable "resource_group_name" {
  description = "Resource group for Terraform state and CI identities."
  type        = string
  default     = "rg-aether-tfstate"
}

variable "storage_account_name" {
  description = "Globally unique storage account name for remote state (lowercase, no hyphens)."
  type        = string
  default     = "aetherstreamtfstate"
}

variable "state_container_name" {
  description = "Blob container name for Terraform state files."
  type        = string
  default     = "tfstate"
}

variable "github_org" {
  description = "GitHub organization or user that owns the repository."
  type        = string
  default     = "asaleh-lab"
}

variable "github_repo" {
  description = "GitHub repository name."
  type        = string
  default     = "AetherStream"
}

variable "tags" {
  description = "Tags applied to bootstrap resources."
  type        = map(string)
  default = {
    project     = "aetherstream"
    environment = "bootstrap"
    managed_by  = "terraform"
  }
}
