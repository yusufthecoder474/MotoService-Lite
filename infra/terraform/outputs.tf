output "resource_group_id" { value = azurerm_resource_group.app.id }
output "container_app_id" { value = azurerm_container_app.app.id }
output "fqdn" {
  value       = azurerm_container_app.app.ingress[0].fqdn
  description = "An assigned hostname is not evidence of healthy or authorized deployment."
}
