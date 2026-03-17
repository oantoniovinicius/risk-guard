package com.galeritos.risk_guard.risk.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.galeritos.risk_guard.TestcontainersConfiguration;
import com.galeritos.risk_guard.banking.application.event.TransactionCreatedEvent;
import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.banking.domain.model.enums.FinancialStatus;
import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.TransactionRepository;
import com.galeritos.risk_guard.config.MessagingProperties;
import com.galeritos.risk_guard.identity.domain.model.User;
import com.galeritos.risk_guard.identity.domain.model.enums.Role;
import com.galeritos.risk_guard.identity.domain.model.enums.UserStatus;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.UserRepository;
import com.galeritos.risk_guard.risk.domain.model.RiskAnalysis;
import com.galeritos.risk_guard.risk.infrastructure.persistence.repository.RiskAnalysisRepository;
import com.galeritos.risk_guard.shared.events.EventTypes;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TransactionCreatedListenerIntegrationTest {
    private static final Duration PROCESSING_TIMEOUT = Duration.ofSeconds(5);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private MessagingProperties messagingProperties;

    @Autowired
    private RiskAnalysisRepository riskAnalysisRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        riskAnalysisRepository.deleteAll();
        transactionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldBeIdempotentWhenReceivingDuplicateTransactionCreatedEvents() throws Exception {
        User sender = userRepository.save(new User(
                null,
                "Risk Sender",
                "risk.sender+" + UUID.randomUUID() + "@example.com",
                "1" + UUID.randomUUID().toString().replace("-", "").substring(0, 10),
                Role.USER,
                UserStatus.ACTIVE));
        User receiver = userRepository.save(new User(
                null,
                "Risk Receiver",
                "risk.receiver+" + UUID.randomUUID() + "@example.com",
                "2" + UUID.randomUUID().toString().replace("-", "").substring(0, 10),
                Role.USER,
                UserStatus.ACTIVE));

        Transaction transaction = transactionRepository.save(new Transaction(
                sender.getId(),
                receiver.getId(),
                new BigDecimal("650.00"),
                TransactionStatus.CREATED,
                FinancialStatus.RESERVED,
                null,
                LocalDateTime.now()));
        UUID transactionId = transaction.getId();

        TransactionCreatedEvent duplicatedEvent = new TransactionCreatedEvent(
                UUID.randomUUID(),
                EventTypes.TRANSACTION_CREATED,
                transactionId,
                new BigDecimal("650.00"),
                sender.getId(),
                receiver.getId());

        rabbitTemplate.convertAndSend(
                messagingProperties.exchange(),
                messagingProperties.routing().transactionCreated(),
                duplicatedEvent);
        rabbitTemplate.convertAndSend(
                messagingProperties.exchange(),
                messagingProperties.routing().transactionCreated(),
                duplicatedEvent);

        awaitRiskAnalysis(transactionId);

        assertEquals(1L, riskAnalysisRepository.count());
        RiskAnalysis riskAnalysis = riskAnalysisRepository.findByTransactionId(transactionId).orElseThrow();
        assertEquals(transactionId, riskAnalysis.getTransactionId());
    }

    private void awaitRiskAnalysis(UUID transactionId) throws InterruptedException {
        long deadline = System.nanoTime() + PROCESSING_TIMEOUT.toNanos();

        while (System.nanoTime() < deadline) {
            if (riskAnalysisRepository.findByTransactionId(transactionId).isPresent()) {
                return;
            }
            Thread.sleep(100);
        }

        fail("Timed out waiting RiskAnalysis creation for transaction " + transactionId);
    }
}
