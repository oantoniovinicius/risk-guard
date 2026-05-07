package com.galeritos.risk_guard.analyst.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.galeritos.risk_guard.analyst.application.usecase.dto.TransactionDetailAggregate;
import com.galeritos.risk_guard.banking.domain.exception.TransactionNotFoundException;
import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.banking.domain.model.TransactionDecisionHistory;
import com.galeritos.risk_guard.banking.domain.model.enums.AnalystDecision;
import com.galeritos.risk_guard.banking.domain.model.enums.FinancialStatus;
import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.TransactionDecisionHistoryRepository;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.TransactionRepository;
import com.galeritos.risk_guard.identity.domain.exception.UserNotFoundException;
import com.galeritos.risk_guard.identity.domain.model.User;
import com.galeritos.risk_guard.identity.domain.model.enums.Role;
import com.galeritos.risk_guard.identity.domain.model.enums.UserStatus;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.UserRepository;
import com.galeritos.risk_guard.risk.domain.model.RiskAnalysis;
import com.galeritos.risk_guard.risk.infrastructure.persistence.repository.RiskAnalysisRepository;
import com.galeritos.risk_guard.shared.enums.RiskLevel;

@ExtendWith(MockitoExtension.class)
class GetAnalystTransactionDetailUseCaseTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RiskAnalysisRepository riskAnalysisRepository;

    @Mock
    private TransactionDecisionHistoryRepository decisionHistoryRepository;

    @InjectMocks
    private GetAnalystTransactionDetailUseCase useCase;

    private Transaction buildTransaction(UUID senderId) {
        UUID transactionId = UUID.randomUUID();
        Transaction t = new Transaction(
                senderId, UUID.randomUUID(), new BigDecimal("700.00"),
                TransactionStatus.AWAITING_ANALYST, FinancialStatus.RESERVED, null, LocalDateTime.now());
        ReflectionTestUtils.setField(t, "id", transactionId);
        return t;
    }

    private User buildUser(UUID userId) {
        return new User(userId, "Ana Lima", "ana@example.com", "12345678900", Role.USER, UserStatus.ACTIVE);
    }

    private RiskAnalysis buildRiskAnalysis(UUID transactionId) {
        return new RiskAnalysis(UUID.randomUUID(), transactionId, new BigDecimal("0.87"),
                RiskLevel.HIGH, "Amount above threshold", "v1.0");
    }

    @Test
    void shouldReturnCompleteAggregateWhenAllDataIsPresent() {
        UUID senderId = UUID.randomUUID();
        Transaction transaction = buildTransaction(senderId);
        UUID transactionId = transaction.getId();
        User sender = buildUser(senderId);
        RiskAnalysis riskAnalysis = buildRiskAnalysis(transactionId);
        List<TransactionDecisionHistory> history = List.of(
                new TransactionDecisionHistory(transactionId, UUID.randomUUID(), AnalystDecision.DENY,
                        TransactionStatus.AWAITING_ANALYST, TransactionStatus.DENIED));

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(riskAnalysisRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(riskAnalysis));
        when(decisionHistoryRepository.findByTransactionIdOrderByCreatedAtDesc(transactionId)).thenReturn(history);

        TransactionDetailAggregate result = useCase.execute(transactionId);

        assertNotNull(result);
        assertEquals(transaction, result.transaction());
        assertEquals(sender, result.sender());
        assertEquals(riskAnalysis, result.riskAnalysis());
        assertEquals(1, result.decisionHistory().size());
    }

    @Test
    void shouldReturnNullRiskAnalysisWhenNotYetAvailable() {
        UUID senderId = UUID.randomUUID();
        Transaction transaction = buildTransaction(senderId);
        UUID transactionId = transaction.getId();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(userRepository.findById(senderId)).thenReturn(Optional.of(buildUser(senderId)));
        when(riskAnalysisRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());
        when(decisionHistoryRepository.findByTransactionIdOrderByCreatedAtDesc(transactionId)).thenReturn(List.of());

        TransactionDetailAggregate result = useCase.execute(transactionId);

        assertNull(result.riskAnalysis());
    }

    @Test
    void shouldReturnEmptyDecisionHistoryForNewTransaction() {
        UUID senderId = UUID.randomUUID();
        Transaction transaction = buildTransaction(senderId);
        UUID transactionId = transaction.getId();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(userRepository.findById(senderId)).thenReturn(Optional.of(buildUser(senderId)));
        when(riskAnalysisRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());
        when(decisionHistoryRepository.findByTransactionIdOrderByCreatedAtDesc(transactionId)).thenReturn(List.of());

        TransactionDetailAggregate result = useCase.execute(transactionId);

        assertEquals(0, result.decisionHistory().size());
    }

    @Test
    void shouldThrowWhenTransactionNotFound() {
        UUID transactionId = UUID.randomUUID();
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class, () -> useCase.execute(transactionId));
    }

    @Test
    void shouldThrowWhenSenderNotFound() {
        UUID senderId = UUID.randomUUID();
        Transaction transaction = buildTransaction(senderId);
        UUID transactionId = transaction.getId();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(userRepository.findById(senderId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> useCase.execute(transactionId));
    }
}
