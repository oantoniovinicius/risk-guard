package com.galeritos.risk_guard.banking.infrastructure.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.galeritos.risk_guard.TestcontainersConfiguration;
import com.galeritos.risk_guard.config.MessagingProperties;
import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.banking.domain.model.enums.FinancialStatus;
import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.AccountRepository;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.TransactionRepository;
import com.galeritos.risk_guard.identity.domain.model.User;
import com.galeritos.risk_guard.identity.domain.model.enums.Role;
import com.galeritos.risk_guard.identity.domain.model.enums.UserStatus;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.UserRepository;
import com.galeritos.risk_guard.risk.infrastructure.persistence.repository.RiskAnalysisRepository;
import com.galeritos.risk_guard.shared.events.EventTypes;

@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, TransferControllerIntegrationTest.RabbitTestConfig.class })
class TransferControllerIntegrationTest {
    private static final Duration MESSAGE_TIMEOUT = Duration.ofSeconds(5);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private RiskAnalysisRepository riskAnalysisRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Autowired
    private Queue transactionCreatedTestQueue;

    @BeforeEach
    void setUp() {
        riskAnalysisRepository.deleteAll();
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();
        rabbitAdmin.purgeQueue(transactionCreatedTestQueue.getName(), true);
    }

    @Test
    void shouldCreateTransferReserveBalanceAndPublishTransactionCreatedMessage() throws Exception {
        BigDecimal transferAmount = new BigDecimal("350.00");

        User sender = userRepository
                .save(new User(null, "Alice Sender", "alice@example.com", "12345678901", Role.USER, UserStatus.ACTIVE));
        User receiver = userRepository
                .save(new User(null, "Bob Receiver", "bob@example.com", "10987654321", Role.USER, UserStatus.ACTIVE));
        UUID senderId = sender.getId();
        UUID receiverId = receiver.getId();

        accountRepository.save(new com.galeritos.risk_guard.banking.domain.model.Account(
                null,
                senderId,
                new BigDecimal("500.00"),
                BigDecimal.ZERO));
        accountRepository.save(new com.galeritos.risk_guard.banking.domain.model.Account(
                null,
                receiverId,
                new BigDecimal("250.00"),
                BigDecimal.ZERO));

        String requestBody = objectMapper
                .writeValueAsString(new TransferRequestPayload(senderId, receiverId, transferAmount));

        mockMvc.perform(post("/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senderId").value(senderId.toString()))
                .andExpect(jsonPath("$.receiverId").value(receiverId.toString()))
                .andExpect(jsonPath("$.amount").value(350.00))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.transactionId").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        com.galeritos.risk_guard.banking.domain.model.Account senderAccount = accountRepository.findByUserId(senderId)
                .orElseThrow();
        assertEquals(0, senderAccount.getBalance().compareTo(new BigDecimal("150.00")));
        assertEquals(0, senderAccount.getReservedBalance().compareTo(transferAmount));
        assertEquals(1L, transactionRepository.count());
        UUID persistedTransactionId = transactionRepository.findAll().get(0).getId();

        Message message = awaitMessage();
        JsonNode eventPayload = objectMapper.readTree(new String(message.getBody(), StandardCharsets.UTF_8));

        assertNotNull(eventPayload.get("eventId").asText());
        assertEquals(EventTypes.TRANSACTION_CREATED, eventPayload.get("eventType").asText());
        assertEquals(persistedTransactionId.toString(), eventPayload.get("aggregateId").asText());
        assertEquals(senderId.toString(), eventPayload.get("senderId").asText());
        assertEquals(receiverId.toString(), eventPayload.get("receiverId").asText());
        assertEquals(0, transferAmount.compareTo(eventPayload.get("amount").decimalValue()));
    }

    private Message awaitMessage() throws InterruptedException {
        long deadline = System.nanoTime() + MESSAGE_TIMEOUT.toNanos();

        while (System.nanoTime() < deadline) {
            Message message = rabbitTemplate.receive(transactionCreatedTestQueue.getName());
            if (message != null) {
                return message;
            }

            Thread.sleep(100);
        }

        throw new AssertionError("Timed out waiting for RabbitMQ message");
    }

    record TransferRequestPayload(UUID senderId, UUID receiverId, BigDecimal amount) {
    }

    record CustomerDecisionPayload(String decision) {
    }

    record AnalystDecisionPayload(String decision) {
    }

    @Test
    void shouldApproveTransactionWhenCustomerConfirms() throws Exception {
        User sender = userRepository
                .save(new User(null, "Sender C1", "sender.c1@example.com", "12345000001", Role.USER, UserStatus.ACTIVE));
        User receiver = userRepository
                .save(new User(null, "Receiver C1", "receiver.c1@example.com", "22345000001", Role.USER, UserStatus.ACTIVE));

        accountRepository.save(new com.galeritos.risk_guard.banking.domain.model.Account(
                null, sender.getId(), new BigDecimal("40.00"), new BigDecimal("125.00")));
        accountRepository.save(new com.galeritos.risk_guard.banking.domain.model.Account(
                null, receiver.getId(), new BigDecimal("10.00"), BigDecimal.ZERO));

        Transaction transaction = transactionRepository.save(new Transaction(
                sender.getId(),
                receiver.getId(),
                new BigDecimal("125.00"),
                TransactionStatus.AWAITING_CUSTOMER,
                FinancialStatus.RESERVED,
                null,
                java.time.LocalDateTime.now()));

        mockMvc.perform(post("/transfers/{transactionId}/customer-confirmation", transaction.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CustomerDecisionPayload("APPROVE"))))
                .andExpect(status().isNoContent());

        Transaction updated = transactionRepository.findById(transaction.getId()).orElseThrow();
        assertEquals(TransactionStatus.APPROVED, updated.getStatus());
        assertEquals(FinancialStatus.SETTLED, updated.getFinancialStatus());

        com.galeritos.risk_guard.banking.domain.model.Account senderAccount = accountRepository
                .findByUserId(sender.getId())
                .orElseThrow();
        com.galeritos.risk_guard.banking.domain.model.Account receiverAccount = accountRepository
                .findByUserId(receiver.getId())
                .orElseThrow();
        assertEquals(0, senderAccount.getBalance().compareTo(new BigDecimal("40.00")));
        assertEquals(0, senderAccount.getReservedBalance().compareTo(BigDecimal.ZERO));
        assertEquals(0, receiverAccount.getBalance().compareTo(new BigDecimal("135.00")));
    }

    @Test
    void shouldConfirmFraudWhenCustomerReportsFraud() throws Exception {
        User sender = userRepository
                .save(new User(null, "Sender C2", "sender.c2@example.com", "12345000002", Role.USER, UserStatus.ACTIVE));
        User receiver = userRepository
                .save(new User(null, "Receiver C2", "receiver.c2@example.com", "22345000002", Role.USER, UserStatus.ACTIVE));

        accountRepository.save(new com.galeritos.risk_guard.banking.domain.model.Account(
                null, sender.getId(), new BigDecimal("40.00"), new BigDecimal("125.00")));
        accountRepository.save(new com.galeritos.risk_guard.banking.domain.model.Account(
                null, receiver.getId(), new BigDecimal("10.00"), BigDecimal.ZERO));

        Transaction transaction = transactionRepository.save(new Transaction(
                sender.getId(),
                receiver.getId(),
                new BigDecimal("125.00"),
                TransactionStatus.AWAITING_CUSTOMER,
                FinancialStatus.RESERVED,
                null,
                java.time.LocalDateTime.now()));

        mockMvc.perform(post("/transfers/{transactionId}/customer-confirmation", transaction.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CustomerDecisionPayload("REPORT_FRAUD"))))
                .andExpect(status().isNoContent());

        Transaction updated = transactionRepository.findById(transaction.getId()).orElseThrow();
        assertEquals(TransactionStatus.FRAUD_CONFIRMED, updated.getStatus());
        assertEquals(FinancialStatus.REVERTED, updated.getFinancialStatus());

        com.galeritos.risk_guard.banking.domain.model.Account senderAccount = accountRepository
                .findByUserId(sender.getId())
                .orElseThrow();
        assertEquals(0, senderAccount.getBalance().compareTo(new BigDecimal("165.00")));
        assertEquals(0, senderAccount.getReservedBalance().compareTo(BigDecimal.ZERO));

        User updatedReceiver = userRepository.findById(receiver.getId()).orElseThrow();
        assertTrue(updatedReceiver.isSuspect());
    }

    @Test
    void shouldDenyTransactionWhenAnalystDenies() throws Exception {
        User sender = userRepository
                .save(new User(null, "Sender A1", "sender.a1@example.com", "12345000003", Role.USER, UserStatus.ACTIVE));
        User receiver = userRepository
                .save(new User(null, "Receiver A1", "receiver.a1@example.com", "22345000003", Role.USER, UserStatus.ACTIVE));

        accountRepository.save(new com.galeritos.risk_guard.banking.domain.model.Account(
                null, sender.getId(), new BigDecimal("40.00"), new BigDecimal("80.00")));
        accountRepository.save(new com.galeritos.risk_guard.banking.domain.model.Account(
                null, receiver.getId(), new BigDecimal("10.00"), BigDecimal.ZERO));

        Transaction transaction = transactionRepository.save(new Transaction(
                sender.getId(),
                receiver.getId(),
                new BigDecimal("80.00"),
                TransactionStatus.AWAITING_ANALYST,
                FinancialStatus.RESERVED,
                null,
                java.time.LocalDateTime.now()));

        mockMvc.perform(post("/transfers/{transactionId}/analyst-decision", transaction.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AnalystDecisionPayload("DENY"))))
                .andExpect(status().isNoContent());

        Transaction updated = transactionRepository.findById(transaction.getId()).orElseThrow();
        assertEquals(TransactionStatus.DENIED, updated.getStatus());
        assertEquals(FinancialStatus.REVERTED, updated.getFinancialStatus());

        com.galeritos.risk_guard.banking.domain.model.Account senderAccount = accountRepository
                .findByUserId(sender.getId())
                .orElseThrow();
        assertEquals(0, senderAccount.getBalance().compareTo(new BigDecimal("120.00")));
        assertEquals(0, senderAccount.getReservedBalance().compareTo(BigDecimal.ZERO));
    }

    @Test
    void shouldApproveTransactionWhenAnalystApproves() throws Exception {
        User sender = userRepository
                .save(new User(null, "Sender A3", "sender.a3@example.com", "12345000005", Role.USER, UserStatus.ACTIVE));
        User receiver = userRepository
                .save(new User(null, "Receiver A3", "receiver.a3@example.com", "22345000005", Role.USER, UserStatus.ACTIVE));

        accountRepository.save(new com.galeritos.risk_guard.banking.domain.model.Account(
                null, sender.getId(), new BigDecimal("40.00"), new BigDecimal("80.00")));
        accountRepository.save(new com.galeritos.risk_guard.banking.domain.model.Account(
                null, receiver.getId(), new BigDecimal("10.00"), BigDecimal.ZERO));

        Transaction transaction = transactionRepository.save(new Transaction(
                sender.getId(),
                receiver.getId(),
                new BigDecimal("80.00"),
                TransactionStatus.AWAITING_ANALYST,
                FinancialStatus.RESERVED,
                null,
                java.time.LocalDateTime.now()));

        mockMvc.perform(post("/transfers/{transactionId}/analyst-decision", transaction.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AnalystDecisionPayload("APPROVE"))))
                .andExpect(status().isNoContent());

        Transaction updated = transactionRepository.findById(transaction.getId()).orElseThrow();
        assertEquals(TransactionStatus.APPROVED, updated.getStatus());
        assertEquals(FinancialStatus.SETTLED, updated.getFinancialStatus());

        com.galeritos.risk_guard.banking.domain.model.Account senderAccount = accountRepository
                .findByUserId(sender.getId())
                .orElseThrow();
        com.galeritos.risk_guard.banking.domain.model.Account receiverAccount = accountRepository
                .findByUserId(receiver.getId())
                .orElseThrow();
        assertEquals(0, senderAccount.getBalance().compareTo(new BigDecimal("40.00")));
        assertEquals(0, senderAccount.getReservedBalance().compareTo(BigDecimal.ZERO));
        assertEquals(0, receiverAccount.getBalance().compareTo(new BigDecimal("90.00")));
    }

    @Test
    void shouldMoveTransactionBackToCustomerWhenAnalystRequestsCustomerConfirmation() throws Exception {
        User sender = userRepository
                .save(new User(null, "Sender A2", "sender.a2@example.com", "12345000004", Role.USER, UserStatus.ACTIVE));
        User receiver = userRepository
                .save(new User(null, "Receiver A2", "receiver.a2@example.com", "22345000004", Role.USER, UserStatus.ACTIVE));

        accountRepository.save(new com.galeritos.risk_guard.banking.domain.model.Account(
                null, sender.getId(), new BigDecimal("40.00"), new BigDecimal("80.00")));
        accountRepository.save(new com.galeritos.risk_guard.banking.domain.model.Account(
                null, receiver.getId(), new BigDecimal("10.00"), BigDecimal.ZERO));

        Transaction transaction = transactionRepository.save(new Transaction(
                sender.getId(),
                receiver.getId(),
                new BigDecimal("80.00"),
                TransactionStatus.AWAITING_ANALYST,
                FinancialStatus.RESERVED,
                null,
                java.time.LocalDateTime.now()));

        java.time.LocalDateTime before = java.time.LocalDateTime.now();

        mockMvc.perform(post("/transfers/{transactionId}/analyst-decision", transaction.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AnalystDecisionPayload("REQUEST_CUSTOMER_CONFIRMATION"))))
                .andExpect(status().isNoContent());

        java.time.LocalDateTime after = java.time.LocalDateTime.now();

        Transaction updated = transactionRepository.findById(transaction.getId()).orElseThrow();
        assertEquals(TransactionStatus.AWAITING_CUSTOMER, updated.getStatus());
        assertEquals(FinancialStatus.RESERVED, updated.getFinancialStatus());
        assertNotNull(updated.getCustomerDecisionDeadlineAt());
        assertTrue(!updated.getCustomerDecisionDeadlineAt().isBefore(before.plusMinutes(10)));
        assertTrue(!updated.getCustomerDecisionDeadlineAt().isAfter(after.plusMinutes(10)));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class RabbitTestConfig {
        @Bean
        Queue transactionCreatedTestQueue() {
            return new AnonymousQueue();
        }

        @Bean
        Binding transactionCreatedTestBinding(
                Queue transactionCreatedTestQueue,
                TopicExchange transactionEventsExchange,
                MessagingProperties properties) {
            return BindingBuilder.bind(transactionCreatedTestQueue)
                    .to(transactionEventsExchange)
                    .with(properties.routing().transactionCreated());
        }
    }
}
