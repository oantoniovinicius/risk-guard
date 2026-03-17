package com.galeritos.risk_guard.risk.infrastructure.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.galeritos.risk_guard.config.MessagingProperties;
import com.galeritos.risk_guard.risk.application.event.TransactionAnalyzedEvent;
import com.galeritos.risk_guard.risk.application.port.out.RiskEventPublisher;

@Component
public class RabbitMqRiskEventPublisher implements RiskEventPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final MessagingProperties messagingProperties;

    public RabbitMqRiskEventPublisher(RabbitTemplate rabbitTemplate, MessagingProperties messagingProperties) {
        this.rabbitTemplate = rabbitTemplate;
        this.messagingProperties = messagingProperties;
    }

    @Override
    public void publishTransactionAnalyzed(TransactionAnalyzedEvent event) {
        rabbitTemplate.convertAndSend(
                messagingProperties.exchange(),
                messagingProperties.routing().transactionAnalyzed(),
                event);
    }
}
