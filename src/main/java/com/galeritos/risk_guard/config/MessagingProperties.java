package com.galeritos.risk_guard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.messaging")
public record MessagingProperties(
        String exchange,
        Consumer consumer,
        Routing routing) {

    public record Consumer(String transactionCreatedQueue, String transactionAnalyzedQueue) {
    }

    public record Routing(String transactionCreated, String transactionAnalyzed) {
    }
}
