package com.galeritos.risk_guard.banking.application.usecase;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.TransactionRepository;

import jakarta.transaction.Transactional;

@Service
public class HandleCustomerConfirmationTimeoutUseCase {
    private final TransactionRepository transactionRepository;

    public HandleCustomerConfirmationTimeoutUseCase(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public int execute(LocalDateTime now) {
        List<UUID> expiredTransactionIds = transactionRepository.findExpiredAwaitingCustomerIds(now);
        int transitioned = 0;

        for (UUID transactionId : expiredTransactionIds) {
            if (moveToAwaitingAnalystWhenExpired(transactionId, now)) {
                transitioned++;
            }
        }

        return transitioned;
    }

    private boolean moveToAwaitingAnalystWhenExpired(UUID transactionId, LocalDateTime now) {
        return transactionRepository.findByIdForUpdate(transactionId)
                .map(transaction -> {
                    if (transaction.getStatus() != TransactionStatus.AWAITING_CUSTOMER) {
                        return false;
                    }

                    if (transaction.getCustomerDecisionDeadlineAt() == null
                            || transaction.getCustomerDecisionDeadlineAt().isAfter(now)) {
                        return false;
                    }

                    transaction.awaitAnalyst();
                    transactionRepository.save(transaction);
                    return true;
                })
                .orElse(false);
    }
}
