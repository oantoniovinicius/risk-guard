package com.galeritos.risk_guard.identity.infrastructure.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.galeritos.risk_guard.config.MessagingProperties;
import com.galeritos.risk_guard.identity.application.event.UserApprovedEvent;
import com.galeritos.risk_guard.identity.application.port.out.IdentityEventPublisher;

@Component
public class RabbitMqIdentityEventPublisher implements IdentityEventPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final MessagingProperties messagingProperties;

    public RabbitMqIdentityEventPublisher(RabbitTemplate rabbitTemplate, MessagingProperties messagingProperties) {
        this.rabbitTemplate = rabbitTemplate;
        this.messagingProperties = messagingProperties;
    }

    @Override
    public void publishUserApproved(UserApprovedEvent event) {
        rabbitTemplate.convertAndSend(
                messagingProperties.exchange(),
                messagingProperties.routing().userApproved(),
                event);
    }
}
