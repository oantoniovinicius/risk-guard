package com.galeritos.risk_guard.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.galeritos.risk_guard.TestcontainersConfiguration;
import com.galeritos.risk_guard.banking.application.event.TransactionCreatedEvent;
import com.galeritos.risk_guard.banking.domain.model.Account;
import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.banking.domain.model.enums.FinancialStatus;
import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.AccountRepository;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.TransactionRepository;
import com.galeritos.risk_guard.config.MessagingProperties;
import com.galeritos.risk_guard.identity.application.event.UserApprovedEvent;
import com.galeritos.risk_guard.identity.domain.model.User;
import com.galeritos.risk_guard.identity.domain.model.enums.Role;
import com.galeritos.risk_guard.identity.domain.model.enums.UserStatus;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.UserRepository;
import com.galeritos.risk_guard.risk.infrastructure.persistence.repository.RiskAnalysisRepository;
import com.galeritos.risk_guard.shared.enums.RiskLevel;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TransactionMessagingIntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RabbitListenerEndpointRegistry listenerRegistry;
    @Autowired
    private MessagingProperties messagingProperties;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private RiskAnalysisRepository riskAnalysisRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User sender;
    private User receiver;

    @BeforeEach
    void setUp() {
        listenerRegistry.stop();
        rabbitTemplate.execute(channel -> {
            channel.queuePurge(messagingProperties.consumer().transactionCreatedQueue());
            channel.queuePurge(messagingProperties.consumer().transactionAnalyzedQueue());
            channel.queuePurge(messagingProperties.consumer().transactionStatusQueue());
            channel.queuePurge(messagingProperties.consumer().userApprovedQueue());
            return null;
        });

        jdbcTemplate.execute("""
                TRUNCATE TABLE transaction_decision_history, transactions, accounts,
                               admin_decision_history, admin_settings, risk_analysis,
                               user_credentials, users RESTART IDENTITY CASCADE
                """);

        sender = userRepository.save(new User(null, "Antonio", "antonio@email.com", "85826257580",
                Role.USER, UserStatus.ACTIVE));

        accountRepository.save(new Account(null, sender.getId(),
                new BigDecimal("500.00"), new BigDecimal("500.00")));

        receiver = userRepository.save(new User(null, "Maria", "maria@email.com", "52998224725",
                Role.USER, UserStatus.ACTIVE));
        accountRepository.save(new Account(null, receiver.getId(),
                new BigDecimal("500.00"), BigDecimal.ZERO));

        listenerRegistry.start();
    }

    @Test
    void lowRiskTransactionShouldBeApprovedAfterPipeline() {
        // Seed one prior transaction between the same pair 2 days ago with the same
        // amount.
        // This makes NewReceiverRule (firstWithReceiver=false) and AmountDeviationRule
        // (ratio=1.0) both return zero, guaranteeing a LOW score even at off-hours.
        transactionRepository.save(new Transaction(
                sender.getId(), receiver.getId(),
                new BigDecimal("50.00"),
                TransactionStatus.APPROVED, FinancialStatus.SETTLED,
                RiskLevel.LOW, LocalDateTime.now().minusDays(2)));

        Transaction transaction = transactionRepository.save(new Transaction(
                sender.getId(), receiver.getId(),
                new BigDecimal("50.00"),
                TransactionStatus.CREATED, FinancialStatus.RESERVED,
                null, LocalDateTime.now()));

        rabbitTemplate.convertAndSend(
                messagingProperties.exchange(),
                messagingProperties.routing().transactionCreated(),
                TransactionCreatedEvent.from(transaction));

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            var analysis = riskAnalysisRepository.findByTransactionId(transaction.getId());
            assertThat(analysis).isPresent();
            assertThat(analysis.get().getRiskLevel()).isEqualTo(RiskLevel.LOW);
        });

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            var updated = transactionRepository.findById(transaction.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(TransactionStatus.APPROVED);
            assertThat(updated.getFinancialStatus()).isEqualTo(FinancialStatus.SETTLED);
        });
    }

    @Test
    void highRiskTransactionShouldAwaitAnalystAfterPipeline() {
        // Seed one FRAUD_CONFIRMED transaction where the receiver was the target.
        // ReceiverProfileRule scores 0.70 for any receiver with confirmed fraud
        // history,
        // pushing the total past the 0.70 HIGH threshold regardless of time of day.
        UUID thirdParty = userRepository.save(new User(null, "Carlos", "carlos@email.com",
                "11144477735", Role.USER, UserStatus.ACTIVE)).getId();
        transactionRepository.save(new Transaction(
                thirdParty, receiver.getId(),
                new BigDecimal("200.00"),
                TransactionStatus.FRAUD_CONFIRMED, FinancialStatus.REVERTED,
                RiskLevel.HIGH, LocalDateTime.now().minusDays(10)));

        Transaction transaction = transactionRepository.save(new Transaction(
                sender.getId(), receiver.getId(),
                new BigDecimal("100.00"),
                TransactionStatus.CREATED, FinancialStatus.RESERVED,
                null, LocalDateTime.now()));

        rabbitTemplate.convertAndSend(
                messagingProperties.exchange(),
                messagingProperties.routing().transactionCreated(),
                TransactionCreatedEvent.from(transaction));

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            var analysis = riskAnalysisRepository.findByTransactionId(transaction.getId());
            assertThat(analysis).isPresent();
            assertThat(analysis.get().getRiskLevel()).isEqualTo(RiskLevel.HIGH);
        });

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            var updated = transactionRepository.findById(transaction.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(TransactionStatus.AWAITING_ANALYST);
        });
    }

    @Test
    void userApprovedEventShouldTriggerAccountCreation() {
        User newUser = userRepository.save(new User(null, "Pedro", "pedro@email.com",
                "11144477735", Role.USER, UserStatus.ACTIVE));

        rabbitTemplate.convertAndSend(
                messagingProperties.exchange(),
                messagingProperties.routing().userApproved(),
                UserApprovedEvent.from(newUser));

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(accountRepository.existsByUserId(newUser.getId())).isTrue();
            var account = accountRepository.findByUserId(newUser.getId()).orElseThrow();
            assertThat(account.getBalance()).isEqualByComparingTo("1000.00");
            assertThat(account.getReservedBalance()).isEqualByComparingTo("0.00");
        });
    }

    @Test
    void duplicateTransactionCreatedEventShouldBeIdempotent() {
        Transaction transaction = transactionRepository.save(new Transaction(
                sender.getId(), receiver.getId(),
                new BigDecimal("50.00"),
                TransactionStatus.CREATED, FinancialStatus.RESERVED,
                null, LocalDateTime.now()));

        TransactionCreatedEvent event = TransactionCreatedEvent.from(transaction);
        rabbitTemplate.convertAndSend(
                messagingProperties.exchange(),
                messagingProperties.routing().transactionCreated(),
                event);
        rabbitTemplate.convertAndSend(
                messagingProperties.exchange(),
                messagingProperties.routing().transactionCreated(),
                event);

        // Wait for the first message to be processed
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(
                () -> assertThat(riskAnalysisRepository.existsByTransactionId(transaction.getId())).isTrue());

        // Wait for the second message to also be consumed (it must return early due to
        // idempotency guard)
        await().during(2, TimeUnit.SECONDS).atMost(8, TimeUnit.SECONDS).until(() -> {
            long count = riskAnalysisRepository.findAll().stream()
                    .filter(a -> a.getTransactionId().equals(transaction.getId()))
                    .count();
            return count == 1L;
        });
    }
}
