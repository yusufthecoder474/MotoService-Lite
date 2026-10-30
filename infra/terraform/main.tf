resource "azurerm_resource_group" "app" {
  name     = "rg-${var.name}"
  location = var.location
  tags     = { project = "motoservice-lite", purpose = "educational-staging" }
  lifecycle {
    precondition {
      condition     = var.cost_approved
      error_message = "STOP: explicit subscription, cost and networking approval is required before plan/apply."
    }
    precondition {
      condition     = !var.public_ingress || length(var.approved_client_cidrs) > 0
      error_message = "Public ingress requires an explicitly approved CIDR allowlist."
    }
  }
}
resource "azurerm_log_analytics_workspace" "app" {
  name                = "log-${var.name}"
  location            = azurerm_resource_group.app.location
  resource_group_name = azurerm_resource_group.app.name
  sku                 = "PerGB2018"
  retention_in_days   = 30
  daily_quota_gb      = 0.5
}
resource "azurerm_container_app_environment" "app" {
  name                       = "env-${var.name}"
  location                   = azurerm_resource_group.app.location
  resource_group_name        = azurerm_resource_group.app.name
  log_analytics_workspace_id = azurerm_log_analytics_workspace.app.id
  infrastructure_subnet_id   = var.infrastructure_subnet_id
}
resource "azurerm_container_app" "app" {
  name                         = var.name
  resource_group_name          = azurerm_resource_group.app.name
  container_app_environment_id = azurerm_container_app_environment.app.id
  revision_mode                = "Single"
  identity {
    type         = "UserAssigned"
    identity_ids = [var.identity_id]
  }
  secret {
    name                = "db-password"
    identity            = var.identity_id
    key_vault_secret_id = var.db_password_secret_id
  }
  ingress {
    external_enabled           = var.public_ingress
    allow_insecure_connections = false
    target_port                = 8080
    transport                  = "http"
    traffic_weight {
      latest_revision = true
      percentage      = 100
    }
    dynamic "ip_security_restriction" {
      for_each = toset(var.approved_client_cidrs)
      content {
        name             = "client-${index(var.approved_client_cidrs, ip_security_restriction.value)}"
        action           = "Allow"
        ip_address_range = ip_security_restriction.value
      }
    }
  }
  template {
    min_replicas = 1
    max_replicas = 1
    container {
      name   = "motoservice"
      image  = var.image
      cpu    = 0.5
      memory = "1Gi"
      env {
        name  = "SPRING_PROFILES_ACTIVE"
        value = "cloud"
      }
      env {
        name  = "DEMO_ENABLED"
        value = "false"
      }
      env {
        name  = "DB_URL"
        value = var.db_jdbc_url
      }
      env {
        name  = "DB_USER"
        value = var.db_user
      }
      env {
        name        = "DB_PASSWORD"
        secret_name = "db-password"
      }
      startup_probe {
        transport               = "HTTP"
        port                    = 8080
        path                    = "/actuator/health/liveness"
        interval_seconds        = 10
        timeout                 = 4
        failure_count_threshold = 30
      }
      liveness_probe {
        transport               = "HTTP"
        port                    = 8080
        path                    = "/actuator/health/liveness"
        interval_seconds        = 10
        timeout                 = 4
        failure_count_threshold = 3
      }
      readiness_probe {
        transport               = "HTTP"
        port                    = 8080
        path                    = "/actuator/health/readiness"
        interval_seconds        = 10
        timeout                 = 4
        failure_count_threshold = 3
      }
    }
  }
  lifecycle { ignore_changes = [secret] }
}
