package com.galeritos.risk_guard.banking.infrastructure.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.galeritos.risk_guard.banking.application.event.TransactionCreatedEvent;
import com.galeritos.risk_guard.banking.application.port.out.EventPublisher;

@Component
public class RabbitMqEventPublisher implements EventPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final RabbitMessagingProperties properties;

    public RabbitMqEventPublisher(RabbitTemplate rabbitTemplate, RabbitMessagingProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @Override
    public void publishTransactionCreated(TransactionCreatedEvent event) {
        rabbitTemplate.convertAndSend(properties.exchange(), properties.routingKey(), event);
    }
}
