package com.galeritos.risk_guard.banking.infrastructure.scheduling;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.galeritos.risk_guard.banking.application.usecase.HandleCustomerConfirmationTimeoutUseCase;

@ExtendWith(MockitoExtension.class)
class CustomerConfirmationTimeoutSchedulerTest {

    @Mock
    private HandleCustomerConfirmationTimeoutUseCase handleCustomerConfirmationTimeoutUseCase;

    @InjectMocks
    private CustomerConfirmationTimeoutScheduler scheduler;

    @Test
    void shouldDelegateToUseCaseWithCurrentTime() {
        scheduler.checkExpiredCustomerConfirmations();

        verify(handleCustomerConfirmationTimeoutUseCase).execute(any(LocalDateTime.class));
    }
}
