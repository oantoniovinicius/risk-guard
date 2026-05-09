package com.galeritos.risk_guard.risk.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.galeritos.risk_guard.banking.application.event.TransactionCreatedEvent;
import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.TransactionRepository;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.UserRepository;
import com.galeritos.risk_guard.risk.application.port.out.RiskScoringContextLoader;
import com.galeritos.risk_guard.risk.domain.scoring.RiskScoringContext;

@Service
public class RiskScoringContextLoaderAdapter implements RiskScoringContextLoader {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public RiskScoringContextLoaderAdapter(
            TransactionRepository transactionRepository,
            UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @Override
    public RiskScoringContext load(TransactionCreatedEvent event) {
        LocalDateTime now = LocalDateTime.now();

        long count1h = transactionRepository.countBySenderIdAndCreatedAtAfter(
                event.senderId(), now.minusHours(1));

        long count24h = transactionRepository.countBySenderIdAndCreatedAtAfter(
                event.senderId(), now.minusHours(24));

        boolean firstWithReceiver = !transactionRepository.existsBySenderIdAndReceiverIdAndIdNot(
                event.senderId(), event.receiverId(), event.aggregateId());

        BigDecimal avgAmount = transactionRepository.findAvgAmountBySenderSince(
                event.senderId(), now.minusDays(30), event.aggregateId())
                .orElse(null);

        long senderFraudCount = transactionRepository.countBySenderIdAndStatus(
                event.senderId(), TransactionStatus.FRAUD_CONFIRMED);

        boolean senderIsSuspect = userRepository.findById(event.senderId())
                .map(u -> u.isSuspect())
                .orElse(false);

        long receiverFraudCount = transactionRepository.countByReceiverIdAndStatus(
                event.receiverId(), TransactionStatus.FRAUD_CONFIRMED);

        boolean receiverIsSuspect = userRepository.findById(event.receiverId())
                .map(u -> u.isSuspect())
                .orElse(false);

        return new RiskScoringContext(
                event.aggregateId(),
                event.amount(),
                now,
                count1h,
                count24h,
                firstWithReceiver,
                avgAmount,
                senderIsSuspect,
                senderFraudCount,
                receiverIsSuspect,
                receiverFraudCount);
    }
}
