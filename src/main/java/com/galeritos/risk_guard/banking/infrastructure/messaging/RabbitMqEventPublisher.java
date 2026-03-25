package com.galeritos.risk_guard.banking.infrastructure.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.galeritos.risk_guard.banking.application.event.TransactionCreatedEvent;
import com.galeritos.risk_guard.banking.application.event.TransactionStatusChangedEvent;
import com.galeritos.risk_guard.banking.application.port.out.BankingEventPublisher;
import com.galeritos.risk_guard.config.MessagingProperties;

@Component
public class RabbitMqEventPublisher implements BankingEventPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final MessagingProperties messagingProperties;

    public RabbitMqEventPublisher(RabbitTemplate rabbitTemplate, MessagingProperties messagingProperties) {
        this.rabbitTemplate = rabbitTemplate;
        this.messagingProperties = messagingProperties;
    }

    @Override
    public void publishTransactionCreated(TransactionCreatedEvent event) {
        rabbitTemplate.convertAndSend(
                messagingProperties.exchange(),
                messagingProperties.routing().transactionCreated(),
                event);
    }

    @Override
    public void publishTransactionStatusChanged(TransactionStatusChangedEvent event) {
        rabbitTemplate.convertAndSend(
                messagingProperties.exchange(),
                event.eventType(),
                event);
    }
}
