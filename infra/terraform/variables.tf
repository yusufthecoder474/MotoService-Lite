variable "subscription_id" { type = string }
variable "location" { type = string }
variable "name" {
  type    = string
  default = "motoservice-staging"
}
variable "cost_approved" {
  type    = bool
  default = false
}
variable "image" {
  type        = string
  description = "Publicly pullable image tagged with the verified full Git commit SHA."
  validation {
    condition     = can(regex(":([0-9a-f]{40})$", var.image))
    error_message = "Use a full 40-character commit SHA image tag."
  }
}
variable "db_jdbc_url" {
  type        = string
  description = "Credential-free JDBC URL with hostname-verifying TLS; use JVM default trust store."
  validation {
    condition     = can(regex("^jdbc:postgresql://[a-zA-Z0-9.-]+(:[0-9]+)?/[a-zA-Z0-9_]+[?]sslmode=verify-full&sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory$", var.db_jdbc_url))
    error_message = "Use the documented verify-full JDBC URL without credentials or extra parameters."
  }
}
variable "db_user" { type = string }
variable "db_password_secret_id" {
  type        = string
  description = "Existing Key Vault HTTPS secret URI, never its value."
  validation {
    condition     = can(regex("^https://[a-zA-Z0-9-]+[.]vault[.]azure[.]net/secrets/", var.db_password_secret_id))
    error_message = "Use an Azure Key Vault secret URI."
  }
}
variable "identity_id" {
  type        = string
  description = "Existing user-assigned identity with narrowly scoped Key Vault secret access."
}
variable "infrastructure_subnet_id" {
  type        = string
  default     = null
  description = "Existing approved ACA subnet when private DB routing is needed. No network is created here."
}
variable "public_ingress" {
  type    = bool
  default = false
}
variable "approved_client_cidrs" {
  type    = list(string)
  default = []
  validation {
    condition     = alltrue([for c in var.approved_client_cidrs : can(cidrnetmask(c)) && c != "0.0.0.0/0"])
    error_message = "Only approved restricted IPv4 CIDRs; never allow the whole internet."
  }
}
