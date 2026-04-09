package com.galeritos.risk_guard.banking.infrastructure.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.galeritos.risk_guard.banking.application.usecase.CreateAccountForApprovedUserUseCase;
import com.galeritos.risk_guard.identity.application.event.UserApprovedEvent;

@Component
public class UserApprovedListener {
    private final CreateAccountForApprovedUserUseCase createAccountForApprovedUserUseCase;

    public UserApprovedListener(CreateAccountForApprovedUserUseCase createAccountForApprovedUserUseCase) {
        this.createAccountForApprovedUserUseCase = createAccountForApprovedUserUseCase;
    }

    @RabbitListener(queues = "${app.messaging.consumer.user-approved-queue}")
    public void handle(UserApprovedEvent event) {
        createAccountForApprovedUserUseCase.execute(event.userId());
    }
}
