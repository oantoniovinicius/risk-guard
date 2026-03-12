package com.galeritos.risk_guard.banking.infrastructure.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.messaging.transaction-created")
public record RabbitMessagingProperties(String exchange, String routingKey) {
}
