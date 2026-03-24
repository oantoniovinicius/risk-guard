package com.galeritos.risk_guard.banking.infrastructure.scheduling;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.galeritos.risk_guard.banking.application.usecase.HandleCustomerConfirmationTimeoutUseCase;

@Component
public class CustomerConfirmationTimeoutScheduler {
    private final HandleCustomerConfirmationTimeoutUseCase handleCustomerConfirmationTimeoutUseCase;

    public CustomerConfirmationTimeoutScheduler(
            HandleCustomerConfirmationTimeoutUseCase handleCustomerConfirmationTimeoutUseCase) {
        this.handleCustomerConfirmationTimeoutUseCase = handleCustomerConfirmationTimeoutUseCase;
    }

    @Scheduled(fixedDelayString = "${banking.customer-confirmation-timeout-check-delay-ms:60000}")
    public void checkExpiredCustomerConfirmations() {
        handleCustomerConfirmationTimeoutUseCase.execute(LocalDateTime.now());
    }
}
